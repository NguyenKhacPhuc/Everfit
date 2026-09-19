package com.example.everfit.assignment.data

import app.cash.turbine.test
import com.example.everfit.assignment.core.model.Result
import com.example.everfit.assignment.data.network.ApiError
import com.example.everfit.assignment.data.local.dao.CompletionDao
import com.example.everfit.assignment.data.local.dao.WorkoutDao
import com.example.everfit.assignment.data.local.entity.CompletionOverrideEntity
import com.example.everfit.assignment.data.local.entity.WorkoutAssignmentEntity
import com.example.everfit.assignment.data.local.entity.WorkoutWithOverride
import com.example.everfit.assignment.core.domain.WorkoutRemoteSource
import com.example.everfit.assignment.core.model.StoredStatus
import com.example.everfit.assignment.core.model.WorkoutAssignment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryTest {

    // ── fakes ─────────────────────────────────────────────────────────────
    // Hand-written rather than mocked: these interfaces have a handful of
    // methods, and a fake is shorter than the stubbing it replaces.

    private class FakeWorkoutDao : WorkoutDao {
        val assignments = MutableStateFlow<List<WorkoutAssignmentEntity>>(emptyList())
        val overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
        var replaceCount = 0

        override fun observeAll(): Flow<List<WorkoutWithOverride>> =
            assignments.map { rows ->
                rows.sortedWith(compareBy({ it.dayIndex }, { it.id })).map {
                    WorkoutWithOverride(
                        it.id, it.dayIndex, it.title, it.serverStatus,
                        it.totalExercises, overrides.value[it.id],
                    )
                }
            }

        override suspend fun deleteAllAssignments() { assignments.value = emptyList() }
        override suspend fun insertAssignments(items: List<WorkoutAssignmentEntity>) {
            assignments.value = assignments.value + items
        }
        override suspend fun replaceAssignments(items: List<WorkoutAssignmentEntity>) {
            replaceCount++
            deleteAllAssignments()
            insertAssignments(items)
        }
    }

    private class FakeCompletionDao(private val dao: FakeWorkoutDao) : CompletionDao {
        override suspend fun overrideFor(assignmentId: String): Boolean? =
            dao.overrides.value[assignmentId]
        override suspend fun upsert(override: CompletionOverrideEntity) {
            dao.overrides.value += override.assignmentId to override.isCompleted
            dao.assignments.value = dao.assignments.value.toList() // trigger re-emission
        }
        override suspend fun delete(assignmentId: String) {
            dao.overrides.value -= assignmentId
            dao.assignments.value = dao.assignments.value.toList()
        }
    }

    /** A source the test can hold open. That is the whole point — see [warmStartEmitsCacheBeforeTheNetworkReturns]. */
    private class FakeRemote : WorkoutRemoteSource {
        var items: List<WorkoutAssignment> = emptyList()
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var calls = 0

        override fun fetchWorkouts(): Flow<List<WorkoutAssignment>> = flow {
            calls++
            gate?.await()
            failure?.let { throw it }
            emit(items)
        }
    }

    private fun assignment(id: String, day: Int, status: StoredStatus = StoredStatus.ASSIGNED) =
        WorkoutAssignment(id, day, "Workout $id", status, totalExercises = 3)

    private fun entity(id: String, day: Int, status: Int = 0) =
        WorkoutAssignmentEntity(id, day, "Workout $id", status, 3)

    private fun repository(dao: FakeWorkoutDao, remote: FakeRemote) =
        WorkoutRepositoryImpl(
            remote = remote,
            workoutDao = dao,
            completionDao = FakeCompletionDao(dao),
            ioDispatcher = UnconfinedTestDispatcher(),
        )

    // ── rung 3.2 ──────────────────────────────────────────────────────────

    @Test
    fun `cold start is empty then populated after a refresh`() = runTest {
        val dao = FakeWorkoutDao()
        val remote = FakeRemote().apply { items = listOf(assignment("a", 0)) }
        val repo = repository(dao, remote)

        repo.observeWeek().test {
            assertEquals(emptyList(), awaitItem())
            repo.refresh()
            assertEquals(listOf("a"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── rung 3.3 — the ordering guarantee ─────────────────────────────────

    /**
     * "Load from cache immediately WHILE fetching" is an ordering guarantee, not
     * an aspiration. The remote is held open, so this can only pass if cached
     * content reaches the collector before the network returns.
     *
     * A fake that returned instantly would pass even against an implementation
     * that awaits the network first — which is the exact bug being guarded.
     */
    @Test
    fun warmStartEmitsCacheBeforeTheNetworkReturns() = runTest {
        val dao = FakeWorkoutDao().apply { assignments.value = listOf(entity("cached", 1)) }
        val gate = CompletableDeferred<Unit>()
        val remote = FakeRemote().apply {
            this.gate = gate
            items = listOf(assignment("fresh", 2))
        }
        val repo = repository(dao, remote)

        repo.observeWeek().test {
            val refreshing = async { repo.refresh() }
            // Let the refresh actually start and park on the gate. Without this
            // the assertion below would prove only that cache arrives before the
            // network *begins*, which is a far weaker claim than the requirement.
            runCurrent()

            // The network has not returned: the gate is still closed.
            assertEquals(listOf("cached"), awaitItem().map { it.id })
            assertTrue(remote.calls == 1, "refresh should already be in flight")
            assertEquals(0, dao.replaceCount, "nothing should be written yet")

            gate.complete(Unit)
            refreshing.await()

            assertEquals(listOf("fresh"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── rung 3.4 — failure is non-destructive ─────────────────────────────

    @Test
    fun `a failed refresh keeps cached content and reports the error`() = runTest {
        val dao = FakeWorkoutDao().apply { assignments.value = listOf(entity("cached", 1)) }
        val remote = FakeRemote().apply { failure = java.net.UnknownHostException("offline") }
        val repo = repository(dao, remote)

        repo.observeWeek().test {
            assertEquals(listOf("cached"), awaitItem().map { it.id })

            val outcome = repo.refresh()

            assertIs<Result.Error>(outcome)
            assertEquals(ApiError.NETWORK, outcome.code)
            assertEquals(0, dao.replaceCount, "a failure must not write")
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── rung 5.5, written here because the repository owns it ─────────────

    @Test
    fun `a refresh does not overwrite a local completion mark`() = runTest {
        val dao = FakeWorkoutDao().apply { assignments.value = listOf(entity("a", 0, status = 0)) }
        val remote = FakeRemote()
        val repo = repository(dao, remote)

        repo.toggleCompletion("a")
        // Server still insists the workout is merely assigned.
        remote.items = listOf(assignment("a", 0, StoredStatus.ASSIGNED))
        repo.refresh()

        repo.observeWeek().test {
            val item = awaitItem().single()
            assertEquals(true, item.localOverride)
            assertTrue(item.isCompleted, "the user's mark must survive a refresh")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling twice returns to not completed`() = runTest {
        val dao = FakeWorkoutDao().apply { assignments.value = listOf(entity("a", 0)) }
        val repo = repository(dao, FakeRemote())

        repo.toggleCompletion("a")
        repo.toggleCompletion("a")

        repo.observeWeek().test {
            assertEquals(false, awaitItem().single().isCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Day 4 of the fixture holds two workouts; only the tapped one may change. */
    @Test
    fun `toggling one workout leaves its neighbour on the same day alone`() = runTest {
        val dao = FakeWorkoutDao().apply {
            assignments.value = listOf(entity("first", 4), entity("second", 4))
        }
        val repo = repository(dao, FakeRemote())

        repo.toggleCompletion("second")

        repo.observeWeek().test {
            val items = awaitItem().associateBy { it.id }
            assertEquals(false, items.getValue("first").isCompleted)
            assertEquals(true, items.getValue("second").isCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

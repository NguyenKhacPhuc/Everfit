package com.example.everfit.assignment.data.remote

import com.example.everfit.assignment.data.mapper.toDomain
import com.example.everfit.assignment.data.network.service.getBaseResponse
import com.example.everfit.assignment.data.network.service.safeApiCall
import com.example.everfit.assignment.data.remote.dto.DayDto
import com.example.everfit.assignment.core.domain.WorkoutRemoteSource
import com.example.everfit.assignment.core.model.WorkoutAssignment
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The only place that knows Ktor exists. Failures are typed by `asResult()`. */
class WorkoutApi(
    private val client: HttpClient,
    private val endpoint: String,
) : WorkoutRemoteSource {

    override fun fetchWorkouts(): Flow<List<WorkoutAssignment>> =
        safeApiCall { client.getBaseResponse<List<DayDto>>(endpoint) }
            .map { days -> days.toDomain() }
}

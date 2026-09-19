package com.example.everfit.assignment.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The store, adapted from the mvi-search reference.
 *
 * Subclasses get a read-only intent stream and one terminal operator, so there
 * is no syntactic path from a feature to the state.
 */
abstract class MviViewModel<I : Any, R : Any, S : Any, E : Any>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    /** For *starting* work only — never for deciding a transition. */
    protected val currentState: S get() = _state.value

    private val _intents = MutableSharedFlow<I>(extraBufferCapacity = 64)
    protected val intents: SharedFlow<I> = _intents.asSharedFlow()

    private val results = Channel<R>(Channel.UNLIMITED)

    private val _effects = Channel<E>(Channel.BUFFERED)

    /** One-shot events. A Channel, not a StateFlow, or they replay on rotation. */
    val effects: Flow<E> = _effects.receiveAsFlow()

    /** The only public door in. */
    fun onIntent(intent: I) {
        _intents.tryEmit(intent)
    }

    /** A function reference, so it has no `this` and cannot stop being pure. */
    protected abstract val reducer: (S, R) -> S

    /** Keeps effects ordered with the state changes that caused them. */
    protected open fun effectFor(result: R, before: S, after: S): E? = null

    init {
        viewModelScope.launch {
            for (result in results) {
                val before = _state.value
                val after = reducer(before, result)
                _state.value = after
                effectFor(result, before, after)?.let { _effects.trySend(it) }
            }
        }
    }

    /** Terminal operator. The only way into the queue, so the only way to affect state. */
    protected fun Flow<R>.pipeToState() = onEach { results.send(it) }.launchIn(viewModelScope)
}

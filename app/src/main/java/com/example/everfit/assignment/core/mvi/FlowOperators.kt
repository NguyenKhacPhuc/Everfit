package com.example.everfit.assignment.core.mvi

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * The flattening strategy the standard library omits: ignore new work while work
 * is running, without cancelling it.
 *
 *   flatMapLatest  cancels the running work    flatMapConcat  queues behind it
 *
 * A double-tapped refresh must do neither.
 */
// Ported from my own mvi-search sample (see README). The standard library has
// no operator that ignores new work while work is running without cancelling it.
fun <T, R> Flow<T>.flatMapFirst(transform: suspend (T) -> Flow<R>): Flow<R> = channelFlow {
    val busy = AtomicBoolean(false)
    collect { value ->
        if (busy.compareAndSet(false, true)) {
            launch {
                try {
                    transform(value).collect { send(it) }
                } finally {
                    busy.set(false)
                }
            }
        }
    }
}

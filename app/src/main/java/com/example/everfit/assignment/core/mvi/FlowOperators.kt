package com.example.everfit.assignment.core.mvi

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * The third flattening strategy, which the standard library omits.
 *
 *   flatMapLatest  cancel the running work, start the new one   → search-as-you-type
 *   flatMapConcat  queue the new work behind the running one    → independent facts
 *   flatMapFirst   ignore the new work while one is running     → refresh, submit
 *
 * A double-tapped refresh must not produce two requests, and must not cancel the
 * first one either — neither built-in operator does that.
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

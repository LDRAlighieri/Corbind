/*
 * Copyright 2021 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.internal

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

/**
 * Creates a hot channel owned by [scope]. Registration and cleanup in [block] run on the Android
 * main thread. The block must suspend with `awaitClose`. Completion of the Job in [scope], including
 * its cancelling phase, cancels the channel and removes the binding. Cancelling the returned channel
 * removes the binding without cancelling [scope].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> corbindReceiveChannel(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    block: suspend ProducerScope<T>.() -> Unit,
): ReceiveChannel<T> {
    val parentJob: Job? = scope.coroutineContext[Job]

    if (parentJob?.isActive == false) {
        return Channel<T>(capacity).apply { cancel(null.scopeCancellationException()) }
    }

    // A detached producer lets a normally completing parent Job finish instead of waiting forever
    // for this binding's awaitClose. The completion handlers below retain one-way ownership.
    val producerScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext = scope.coroutineContext.minusKey(Job)
    }

    val channel: ReceiveChannel<T> =
        producerScope.produce(context = Dispatchers.Main.immediate, capacity) {
            if (parentJob?.isActive == false) throw null.scopeCancellationException()
            block()
        }

    parentJob?.cancelChannelOnCompletion(channel, channel as Job)

    return channel
}

@OptIn(InternalCoroutinesApi::class)
private fun Job.cancelChannelOnCompletion(
    channel: ReceiveChannel<*>,
    channelJob: Job,
) {
    val parentHandle = CompletionHandle()
    channelJob.invokeOnCompletion { parentHandle.dispose() }

    // Observe the cancelling phase so a non-cooperative sibling cannot keep an Android callback
    // registered indefinitely. This handler also runs once on normal Job completion.
    parentHandle.attach(
        invokeOnCompletion(onCancelling = true, invokeImmediately = true) { cause ->
            channel.cancel(cause.scopeCancellationException())
        },
    )
}

private class CompletionHandle {

    private var handle: DisposableHandle? = null
    private var disposed = false

    fun attach(handle: DisposableHandle) {
        val disposeNow: Boolean = synchronized(this) {
            if (disposed) {
                true
            } else {
                this.handle = handle
                false
            }
        }

        if (disposeNow) handle.dispose()
    }

    fun dispose() {
        val handleToDispose: DisposableHandle? = synchronized(this) {
            if (disposed) return
            disposed = true
            handle.also { handle = null }
        }

        handleToDispose?.dispose()
    }
}

private fun Throwable?.scopeCancellationException(): CancellationException =
    when (val cause = this) {
        is CancellationException -> cause
        null -> CancellationException("The owning CoroutineScope has completed")
        else -> CancellationException("The owning CoroutineScope has completed").apply(::initCause)
    }

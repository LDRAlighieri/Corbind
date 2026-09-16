/*
 * Copyright 2026 Vladimir Raupov
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates a non-blocking callback emitter which honors the channel's configured overflow policy.
 * A value that cannot be sent immediately with a suspending overflow policy is queued by an
 * undispatched child coroutine. This preserves callback order without blocking the callback thread.
 * Explicit conflation and drop policies keep their regular channel semantics.
 *
 * The returned function reports whether the configured policy accepted the value immediately or
 * queued it while [owner] was active. Pending values are cancelled with [owner]. Every pending
 * value retains a suspended child, so callers should choose conflation or a drop policy when an
 * unbounded backlog is not acceptable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> SendChannel<T>.corbindEventEmitter(owner: CoroutineScope): (T) -> Boolean = { value ->
    if (!owner.isActive) {
        false
    } else {
        val result = trySend(value)
        when {
            result.isSuccess -> true
            result.isClosed -> false
            else -> queueSend(owner, value)
        }
    }
}

/** Creates a callback emitter owned by this producer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> ProducerScope<T>.corbindEventEmitter(): (T) -> Boolean = corbindEventEmitter(this)

private fun <T> SendChannel<T>.queueSend(
    owner: CoroutineScope,
    value: T,
): Boolean {
    val delivered = AtomicBoolean()
    val sender = owner.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            ensureActive()
            send(value)
            delivered.set(true)
        } catch (_: Exception) {
            // Closing or cancelling the binding rejects a pending value without failing its owner.
        }
    }

    return sender.isActive || delivered.get()
}

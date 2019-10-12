/*
 * Copyright 2019 Vladimir Raupov
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

package ru.ldralighieri.corbind.view

import android.view.DragEvent
import android.view.View
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on [DragEvent] for [View].
 *
 * *Warning:* The created actor uses [View.setOnDragListener]. Only one actor can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnDragListener]
 * @param action An action to perform
 */
fun View.drags(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (DragEvent) -> Boolean = AlwaysTrue,
    action: suspend (DragEvent) -> Unit
) {
    val events = scope.actor<DragEvent>(Dispatchers.Main.immediate, capacity) {
        for (drag in channel) action(drag)
    }

    setOnDragListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnDragListener(null) }
}

/**
 * Perform an action on [DragEvent] for [View], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnDragListener]. Only one actor can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnDragListener]
 * @param action An action to perform
 */
suspend fun View.drags(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (DragEvent) -> Boolean = AlwaysTrue,
    action: suspend (DragEvent) -> Unit
) = coroutineScope {
    drags(this, capacity, handled, action)
}

/**
 * Create a channel of [DragEvent] for [View].
 *
 * *Warning:* The created channel uses [View.setOnDragListener]. Only one channel can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.drags(scope)
 *          .consumeEach { /* handle drag */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnDragListener]
 */
@CheckResult
fun View.drags(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (DragEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<DragEvent> = corbindReceiveChannel(capacity) {
    setOnDragListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnDragListener(null) }
}

/**
 * Create a flow of [DragEvent] for [View].
 *
 * *Warning:* The created flow uses [View.setOnDragListener]. Only one flow can be used at a time.
 *
 * Example:
 *
 * ```
 * view.drags()
 *      .onEach { /* handle drag */ }
 *      .launchIn(scope)
 * ```
 *
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnDragListener]
 */
@CheckResult
fun View.drags(
    handled: (DragEvent) -> Boolean = AlwaysTrue
): Flow<DragEvent> = channelFlow {
    setOnDragListener(listener(this, handled, ::offer))
    awaitClose { setOnDragListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (DragEvent) -> Boolean,
    emitter: (DragEvent) -> Boolean
) = View.OnDragListener { _, dragEvent ->
    if (scope.isActive) {
        if (handled(dragEvent)) {
            emitter(dragEvent)
            return@OnDragListener true
        }
    }
    return@OnDragListener false
}

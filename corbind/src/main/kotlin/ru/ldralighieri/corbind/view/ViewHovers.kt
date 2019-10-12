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

import android.view.MotionEvent
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
 * Perform an action on hover events for [View].
 *
 * *Warning:* The created actor uses [View.setOnHoverListener]. Only one actor can be used at a
 * time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnHoverListener]
 * @param action An action to perform
 */
fun View.hovers(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
    action: suspend (MotionEvent) -> Unit
) {
    val events = scope.actor<MotionEvent>(Dispatchers.Main.immediate, capacity) {
        for (motion in channel) action(motion)
    }

    setOnHoverListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnHoverListener(null) }
}

/**
 * Perform an action on hover events for [View], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnHoverListener]. Only one actor can be used at a
 * time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnHoverListener]
 * @param action An action to perform
 */
suspend fun View.hovers(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
    action: suspend (MotionEvent) -> Unit
) = coroutineScope {
    hovers(this, capacity, handled, action)
}

/**
 * Create a channel of hover events for [View].
 *
 * *Warning:* The created channel uses [View.setOnHoverListener]. Only one channel can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.hovers(scope)
 *          .consumeEach { /* handle hover */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnHoverListener]
 */
@CheckResult
fun View.hovers(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = corbindReceiveChannel(capacity) {
    setOnHoverListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnHoverListener(null) }
}

/**
 * Create a flow of hover events for [View].
 *
 * *Warning:* The created flow uses [View.setOnHoverListener]. Only one flow can be used at a time.
 *
 * Example:
 *
 * ```
 * view.hovers()
 *      .onEach { /* handle hover */ }
 *      .launchIn(scope)
 * ```
 *
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnHoverListener]
 */
@CheckResult
fun View.hovers(
    handled: (MotionEvent) -> Boolean = AlwaysTrue
): Flow<MotionEvent> = channelFlow {
    setOnHoverListener(listener(this, handled, ::offer))
    awaitClose { setOnHoverListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MotionEvent) -> Boolean,
    emitter: (MotionEvent) -> Boolean
) = View.OnHoverListener { _, motionEvent ->
    if (scope.isActive) {
        if (handled(motionEvent)) {
            emitter(motionEvent)
            return@OnHoverListener true
        }
    }
    return@OnHoverListener false
}

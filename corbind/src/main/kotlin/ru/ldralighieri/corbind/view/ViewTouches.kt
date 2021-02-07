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

import android.annotation.SuppressLint
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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on touch events for [View].
 *
 * *Warning:* The created actor uses [View.setOnTouchListener]. Only one actor can be used at a
 * time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 * @param action An action to perform
 */
fun View.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
    action: suspend (MotionEvent) -> Unit
) {
    val events = scope.actor<MotionEvent>(Dispatchers.Main.immediate, capacity) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnTouchListener(null) }
}

/**
 * Perform an action on touch events for [View], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnTouchListener]. Only one actor can be used at a
 * time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 * @param action An action to perform
 */
suspend fun View.touches(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
    action: suspend (MotionEvent) -> Unit
) = coroutineScope {
    touches(this, capacity, handled, action)
}

/**
 * Create a channel of touch events for [View].
 *
 * *Warning:* The created channel uses [View.setOnTouchListener]. Only one channel can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.touches(scope)
 *          .consumeEach { /* handle touch */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 */
@CheckResult
fun View.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MotionEvent> = corbindReceiveChannel(capacity) {
    setOnTouchListener(listener(scope, handled, ::offerCatching))
    invokeOnClose { setOnTouchListener(null) }
}

/**
 * Create a flow of touch events for [View].
 *
 * *Warning:* The created flow uses [View.setOnTouchListener]. Only one flow can be used at a time.
 *
 * Example:
 *
 * ```
 * view.touches()
 *      .onEach { /* handle touch */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]
 */
@CheckResult
fun View.touches(
    handled: (MotionEvent) -> Boolean = AlwaysTrue
): Flow<MotionEvent> = channelFlow<MotionEvent> {
    setOnTouchListener(listener(this, handled, ::offerCatching))
    awaitClose { setOnTouchListener(null) }
}

@SuppressLint("ClickableViewAccessibility")
@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MotionEvent) -> Boolean,
    emitter: (MotionEvent) -> Boolean
) = View.OnTouchListener { _, motionEvent ->

    if (scope.isActive && handled(motionEvent)) {
        emitter(motionEvent)
        return@OnTouchListener true
    }
    return@OnTouchListener false
}

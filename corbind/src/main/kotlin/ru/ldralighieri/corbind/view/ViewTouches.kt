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
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action on touch events for [View].
 *
 * *Warning:* The created actor uses [View.setOnTouchListener]. Only one actor can be used at a
 * time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
fun View.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
    action: suspend (MotionEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<MotionEvent>(Dispatchers.Main.immediate, capacity) {
        for (motion in channel) action(motion)
    }

    setOnTouchListener(listener(scope, handled, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnTouchListener(null) }
}

/**
 * Perform an action on touch events for [View], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnTouchListener]. Only one actor can be used at a
 * time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
suspend fun View.touches(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
    action: suspend (MotionEvent) -> Unit,
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun View.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
): ReceiveChannel<MotionEvent> = corbindReceiveChannel(scope, capacity) {
    setOnTouchListener(listener(scope, handled, corbindEventEmitter()))
    awaitClose { setOnTouchListener(null) }
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
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Predicate invoked with each value to determine the return value of the underlying
 * [View.OnTouchListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun View.touches(
    handled: (MotionEvent) -> Boolean = AlwaysTrue,
): Flow<MotionEvent> = corbindCallbackFlow {
    setOnTouchListener(listener(this, handled, corbindEventEmitter()))
    awaitClose { setOnTouchListener(null) }
}

@SuppressLint("ClickableViewAccessibility")
@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MotionEvent) -> Boolean,
    emitter: (MotionEvent) -> Boolean,
) = View.OnTouchListener { _, motionEvent ->

    if (scope.isActive && handled(motionEvent)) {
        return@OnTouchListener emitter(motionEvent)
    }
    return@OnTouchListener false
}

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
 * Perform an action on [View] long click events.
 *
 * *Warning:* The created actor uses [View.setOnLongClickListener]. Only one actor can be used at a
 * time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
fun View.longClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setOnLongClickListener(listener(scope, handled, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnLongClickListener(null) }
}

/**
 * Perform an action on [View] long click events, in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnLongClickListener]. Only one actor can be used at a
 * time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
suspend fun View.longClicks(
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend () -> Unit,
) = coroutineScope {
    longClicks(this, capacity, handled, action)
}

/**
 * Create a channel that emits [View] long click events.
 *
 * *Warning:* The created channel uses [View.setOnLongClickListener]. Only one channel can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.longClicks(scope)
 *          .consumeEach { /* handle long click */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun View.longClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    setOnLongClickListener(listener(scope, handled, corbindEventEmitter()))
    awaitClose { setOnLongClickListener(null) }
}

/**
 * Create a flow that emits [View] long click events.
 *
 * *Warning:* The created flow uses [View.setOnLongClickListener]. Only one flow can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * view.longClicks()
 *      .onEach { /* handle long click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun View.longClicks(
    handled: () -> Boolean = AlwaysTrue,
): Flow<Unit> = corbindCallbackFlow {
    setOnLongClickListener(listener(this, handled, corbindEventEmitter()))
    awaitClose { setOnLongClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: () -> Boolean,
    emitter: (Unit) -> Boolean,
) = View.OnLongClickListener {
    if (scope.isActive && handled()) {
        return@OnLongClickListener emitter(Unit)
    }
    return@OnLongClickListener false
}

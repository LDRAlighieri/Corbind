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
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

data class ViewLayoutChangeEvent(
    val view: View,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val oldLeft: Int,
    val oldTop: Int,
    val oldRight: Int,
    val oldBottom: Int,
)

/**
 * Perform an action on [layout change events][ViewLayoutChangeEvent] for [View].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun View.layoutChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewLayoutChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<ViewLayoutChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnLayoutChangeListener(listener)
    events.invokeOnCloseOnMain { removeOnLayoutChangeListener(listener) }
}

/**
 * Perform an action on [layout change events][ViewLayoutChangeEvent] for [View], in a new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun View.layoutChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewLayoutChangeEvent) -> Unit,
) = coroutineScope {
    layoutChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [layout change events][ViewLayoutChangeEvent] for [View].
 *
 * Example:
 *
 * ```
 * launch {
 *      view.layoutChangeEvents(scope)
 *          .consumeEach { /* handle layout change event */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun View.layoutChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<ViewLayoutChangeEvent> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addOnLayoutChangeListener(listener)
    awaitClose { removeOnLayoutChangeListener(listener) }
}

/**
 * Create a flow of [layout change events][ViewLayoutChangeEvent] for [View].
 *
 * Example:
 *
 * ```
 * view.layoutChangeEvents()
 *      .onEach { /* handle layout change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.layoutChangeEvents(): Flow<ViewLayoutChangeEvent> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addOnLayoutChangeListener(listener)
    awaitClose { removeOnLayoutChangeListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (ViewLayoutChangeEvent) -> Boolean,
) = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
    if (scope.isActive) {
        emitter(
            ViewLayoutChangeEvent(
                view = v,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                oldLeft = oldLeft,
                oldTop = oldTop,
                oldRight = oldRight,
                oldBottom = oldBottom,
            ),
        )
    }
}

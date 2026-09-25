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

data class ViewScrollChangeEvent(
    val view: View,
    val scrollX: Int,
    val scrollY: Int,
    val oldScrollX: Int,
    val oldScrollY: Int,
)

/**
 * Perform an action on [scroll change events][ViewScrollChangeEvent] for [View].
 *
 * *Warning:* The created actor uses [View.setOnScrollChangeListener]. Only one actor can be used at
 * a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun View.scrollChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewScrollChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<ViewScrollChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnScrollChangeListener(null) }
}

/**
 * Perform an action on [scroll change events][ViewScrollChangeEvent] for [View], in a new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnScrollChangeListener]. Only one actor can be used at
 * a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun View.scrollChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewScrollChangeEvent) -> Unit,
) = coroutineScope {
    scrollChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [scroll change events][ViewScrollChangeEvent] for [View].
 *
 * *Warning:* The created channel uses [View.setOnScrollChangeListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.scrollChangeEvents(scope)
 *          .consumeEach { /* handle scroll change event */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun View.scrollChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<ViewScrollChangeEvent> = corbindReceiveChannel(scope, capacity) {
    setOnScrollChangeListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnScrollChangeListener(null) }
}

/**
 * Create a flow of [scroll change events][ViewScrollChangeEvent] for [View].
 *
 * *Warning:* The created flow uses [View.setOnScrollChangeListener]. Only one flow can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * view.scrollChangeEvents()
 *      .onEach { /* handle scroll change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.scrollChangeEvents(): Flow<ViewScrollChangeEvent> = corbindCallbackFlow {
    setOnScrollChangeListener(listener(this, corbindEventEmitter()))
    awaitClose { setOnScrollChangeListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (ViewScrollChangeEvent) -> Boolean,
) = View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
    if (scope.isActive) {
        emitter(ViewScrollChangeEvent(v, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}

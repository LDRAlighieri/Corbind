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

package ru.ldralighieri.corbind.appcompat

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.appcompat.widget.PopupMenu
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

/**
 * Perform an action on [PopupMenu] dismiss events.
 *
 * *Warning:* The created actor uses [PopupMenu.setOnDismissListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun PopupMenu.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setOnDismissListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnDismissListener(null) }
}

/**
 * Perform an action on [PopupMenu] dismiss events, in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [PopupMenu.setOnDismissListener]. Only one actor can be used
 * for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun PopupMenu.dismisses(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    dismisses(this, capacity, action)
}

/**
 * Create a channel that emits [PopupMenu] dismiss events.
 *
 * *Warning:* The created channel uses [PopupMenu.setOnDismissListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      popupMenu.dismisses(scope)
 *          .consumeEach { /* handle popup menu dismiss */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun PopupMenu.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    setOnDismissListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnDismissListener(null) }
}

/**
 * Create a flow that emits [PopupMenu] dismiss events.
 *
 * *Warning:* The created flow uses [PopupMenu.setOnDismissListener]. Only one flow can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * popupMenu.dismisses()
 *      .onEach { /* handle popup menu dismiss */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun PopupMenu.dismisses(): Flow<Unit> = corbindCallbackFlow {
    setOnDismissListener(listener(this, corbindEventEmitter()))
    awaitClose { setOnDismissListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean,
) = PopupMenu.OnDismissListener {
    if (scope.isActive) emitter(Unit)
}

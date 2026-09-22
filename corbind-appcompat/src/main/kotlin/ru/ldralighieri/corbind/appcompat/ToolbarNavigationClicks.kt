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

import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
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
 * Perform an action on [Toolbar] navigation click events.
 *
 * *Warning:* The created actor uses [Toolbar.setNavigationOnClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun Toolbar.navigationClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setNavigationOnClickListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setNavigationOnClickListener(null) }
}

/**
 * Perform an action on [Toolbar] navigation click events, in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [Toolbar.setNavigationOnClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun Toolbar.navigationClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    navigationClicks(this, capacity, action)
}

/**
 * Create a channel that emits [Toolbar] navigation click events.
 *
 * *Warning:* The created channel uses [Toolbar.setNavigationOnClickListener]. Only one channel can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      toolbar.navigationClicks(scope)
 *          .consumeEach { /* handle navigation click */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun Toolbar.navigationClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    setNavigationOnClickListener(listener(scope, corbindEventEmitter()))
    awaitClose { setNavigationOnClickListener(null) }
}

/**
 * Create a flow that emits [Toolbar] navigation click events.
 *
 * *Warning:* The created flow uses [Toolbar.setNavigationOnClickListener]. Only one flow can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * toolbar.navigationClicks()
 *      .onEach { /* handle navigation click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun Toolbar.navigationClicks(): Flow<Unit> = corbindCallbackFlow {
    setNavigationOnClickListener(listener(this, corbindEventEmitter()))
    awaitClose { setNavigationOnClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean,
) = View.OnClickListener {
    if (scope.isActive) emitter(Unit)
}

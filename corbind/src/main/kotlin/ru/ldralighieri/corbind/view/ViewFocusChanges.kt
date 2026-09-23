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
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on [View] focus change.
 *
 * *Warning:* The created actor uses [View.setOnFocusChangeListener]. Only one actor can be used at
 * a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun View.focusChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (focus in channel) action(focus)
    }

    events.corbindEventEmitter(scope)(hasFocus())
    onFocusChangeListener = listener(scope, events.corbindEventEmitter(scope))
    events.invokeOnCloseOnMain { onFocusChangeListener = null }
}

/**
 * Perform an action on [View] focus change, in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnFocusChangeListener]. Only one actor can be used at
 * a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun View.focusChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) = coroutineScope {
    focusChanges(this, capacity, action)
}

/**
 * Create a channel of booleans representing the focus of [View].
 *
 * *Warning:* The created channel uses [View.setOnFocusChangeListener]. Only one channel can be used
 * at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.focusChanges(scope)
 *          .consumeEach { /* handle focus change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun View.focusChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Boolean> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(hasFocus())
    onFocusChangeListener = listener(scope, corbindEventEmitter())
    awaitClose { onFocusChangeListener = null }
}

/**
 * Create a flow of booleans representing the focus of [View].
 *
 * *Warning:* The created flow uses [View.setOnFocusChangeListener]. Only one flow can be used at a
 * time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * view.focusChanges()
 *      .onEach { /* handle focus change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * view.focusChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle focus change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.focusChanges(): InitialValueFlow<Boolean> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    onFocusChangeListener = listener(this, emitter)
    emitter.sendInitialValue(hasFocus())
    awaitClose { onFocusChangeListener = null }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean,
) = View.OnFocusChangeListener { _, hasFocus ->
    if (scope.isActive) emitter(hasFocus)
}

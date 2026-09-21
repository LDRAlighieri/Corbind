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

package ru.ldralighieri.corbind.widget

import android.widget.CompoundButton
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
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on checked state of [CompoundButton].
 *
 * *Warning:* The created actor uses [CompoundButton.setOnCheckedChangeListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun CompoundButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (checked in channel) action(checked)
    }

    events.corbindEventEmitter(scope)(isChecked)
    setOnCheckedChangeListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked state of [CompoundButton], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [CompoundButton.setOnCheckedChangeListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun CompoundButton.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel of booleans representing the checked state of [CompoundButton].
 *
 * *Warning:* The created channel uses [CompoundButton.setOnCheckedChangeListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      compoundButton.checkedChanges(scope)
 *          .consumeEach { /* handle checked change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun CompoundButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Boolean> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(isChecked)
    setOnCheckedChangeListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnCheckedChangeListener(null) }
}

/**
 * Create a flow of booleans representing the checked state of [CompoundButton].
 *
 * *Warning:* The created flow uses [CompoundButton.setOnCheckedChangeListener]. Only one flow can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * compoundButton.checkedChanges()
 *      .onEach { /* handle checked change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * compoundButton.checkedChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle checked change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun CompoundButton.checkedChanges(): InitialValueFlow<Boolean> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    setOnCheckedChangeListener(listener(this, emitter))
    emitter.sendInitialValue(isChecked)
    awaitClose { setOnCheckedChangeListener(null) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean,
) = CompoundButton.OnCheckedChangeListener { _, isChecked ->
    if (scope.isActive) emitter(isChecked)
}

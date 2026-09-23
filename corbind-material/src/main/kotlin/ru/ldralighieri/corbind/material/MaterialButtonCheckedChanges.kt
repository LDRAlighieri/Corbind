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

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import com.google.android.material.button.MaterialButton
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
 * Perform an action on [MaterialButton] check state changes.
 *
 * *Warning:* Requires a checkable [MaterialButton]; otherwise throws [IllegalStateException].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun MaterialButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (checked in channel) action(checked)
    }

    checkCheckableState(this)
    events.corbindEventEmitter(scope)(isChecked)
    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnCheckedChangeListener(listener)
    events.invokeOnCloseOnMain { removeOnCheckedChangeListener(listener) }
}

/**
 * Perform an action on [MaterialButton] check state changes, in a new [CoroutineScope].
 *
 * *Warning:* Requires a checkable [MaterialButton]; otherwise throws [IllegalStateException].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun MaterialButton.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel that emits [MaterialButton] check state changes.
 *
 * *Warning:* Requires a checkable [MaterialButton]; otherwise throws [IllegalStateException].
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialButton.checkedChanges(scope)
 *          .consumeEach { /* handle check state change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun MaterialButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Boolean> = corbindReceiveChannel(scope, capacity) {
    checkCheckableState(this@checkedChanges)
    sendInitialValue(isChecked)
    val listener = listener(scope, corbindEventEmitter())
    addOnCheckedChangeListener(listener)
    awaitClose { removeOnCheckedChangeListener(listener) }
}

/**
 * Create a flow that emits [MaterialButton] check state changes.
 *
 * *Warning:* Requires a checkable [MaterialButton]; otherwise throws [IllegalStateException].
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * materialButton.checkedChanges()
 *      .onEach { /* handle check state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * materialButton.checkedChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle check state change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun MaterialButton.checkedChanges(): InitialValueFlow<Boolean> = corbindCallbackFlow {
    checkCheckableState(this@checkedChanges)
    val emitter = initialValueFlowEmitter()
    val listener = listener(this, emitter)
    addOnCheckedChangeListener(listener)
    emitter.sendInitialValue(isChecked)
    awaitClose { removeOnCheckedChangeListener(listener) }
}.asInitialValueFlow()

private fun checkCheckableState(button: MaterialButton) {
    check(button.isCheckable) { "The MaterialButton is not in checkable state" }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean,
) = MaterialButton.OnCheckedChangeListener { _, isChecked ->
    if (scope.isActive) emitter(isChecked)
}

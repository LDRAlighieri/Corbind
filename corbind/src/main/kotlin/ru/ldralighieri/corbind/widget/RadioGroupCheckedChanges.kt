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

import android.view.View
import android.widget.RadioGroup
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
 * Perform an action on checked view ID changes in [RadioGroup].
 *
 * *Warning:* The created actor uses [RadioGroup.setOnCheckedChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun RadioGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    events.corbindEventEmitter(scope)(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked view ID changes in [RadioGroup], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [RadioGroup.setOnCheckedChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun RadioGroup.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel of the checked view ID changes in [RadioGroup].
 *
 * *Warning:* The created channel uses [RadioGroup.setOnCheckedChangeListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately. When the selection is cleared, checkedId is
 * [View.NO_ID]
 *
 * Example:
 *
 * ```
 * launch {
 *      radioGroup.checkedChanges(scope)
 *          .consumeEach { /* handle checked change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun RadioGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnCheckedChangeListener(null) }
}

/**
 * Create a flow of the checked view ID changes in [RadioGroup].
 *
 * *Warning:* The created flow uses [RadioGroup.setOnCheckedChangeListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately. When the selection is cleared, checkedId is
 * [View.NO_ID]
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * radioGroup.checkedChanges()
 *      .onEach { /* handle checked change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * radioGroup.checkedChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle checked change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RadioGroup.checkedChanges(): InitialValueFlow<Int> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    setOnCheckedChangeListener(listener(this, emitter))
    emitter.sendInitialValue(checkedRadioButtonId)
    awaitClose { setOnCheckedChangeListener(null) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = object : RadioGroup.OnCheckedChangeListener {

    private var lastChecked = View.NO_ID
    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (scope.isActive && checkedId != lastChecked) {
            lastChecked = checkedId
            emitter(checkedId)
        }
    }
}

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
import android.widget.Adapter
import android.widget.AdapterView
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
 * Perform an action on the selected position of [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemSelectedListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun <T : Adapter> AdapterView<T>.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (position in channel) action(position)
    }

    events.corbindEventEmitter(scope)(selectedItemPosition)
    onItemSelectedListener = listener(scope, events.corbindEventEmitter(scope))
    events.invokeOnCloseOnMain { onItemSelectedListener = null }
}

/**
 * Perform an action on the selected position of [AdapterView], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemSelectedListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun <T : Adapter> AdapterView<T>.itemSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    itemSelections(this, capacity, action)
}

/**
 * Create a channel of the selected position of [AdapterView]. If nothing is selected,
 * [AdapterView.INVALID_POSITION] will be emitted.
 *
 * *Warning:* The created channel uses [AdapterView.setOnItemSelectedListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      adapterView.itemSelections(scope)
 *          .consumeEach { /* handle selected position */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(selectedItemPosition)
    onItemSelectedListener = listener(scope, corbindEventEmitter())
    awaitClose { onItemSelectedListener = null }
}

/**
 * Create a flow of the selected position of [AdapterView]. If nothing is selected,
 * [AdapterView.INVALID_POSITION] will be emitted.
 *
 * *Warning:* The created flow uses [AdapterView.setOnItemSelectedListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * adapterView.itemSelections()
 *      .onEach { /* handle selected position */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * adapterView.itemSelections()
 *      .dropInitialValue()
 *      .onEach { /* handle selected position */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(): InitialValueFlow<Int> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    onItemSelectedListener = listener(this, emitter)
    emitter.sendInitialValue(selectedItemPosition)
    awaitClose { onItemSelectedListener = null }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        onEvent(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>) = onEvent(AdapterView.INVALID_POSITION)

    private fun onEvent(position: Int) {
        if (scope.isActive) emitter(position)
    }
}

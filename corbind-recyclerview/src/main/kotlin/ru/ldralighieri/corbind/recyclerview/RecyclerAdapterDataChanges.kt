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

package ru.ldralighieri.corbind.recyclerview

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
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
 * Perform an action when `AdapterDataObserver.onChanged()` reports a full data set change for
 * [RecyclerView.Adapter].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (T) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<T>(Dispatchers.Main.immediate, capacity) {
        for (adapter in channel) action(adapter)
    }

    events.corbindEventEmitter(scope)(this)
    val dataObserver = observer(scope, this, events.corbindEventEmitter(scope))
    registerAdapterDataObserver(dataObserver)
    events.invokeOnCloseOnMain { unregisterAdapterDataObserver(dataObserver) }
}

/**
 * Perform an action when `AdapterDataObserver.onChanged()` reports a full data set change for
 * [RecyclerView.Adapter], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (T) -> Unit,
) = coroutineScope {
    dataChanges(this, capacity, action)
}

/**
 * Create a channel that emits [RecyclerView.Adapter] when `AdapterDataObserver.onChanged()` reports
 * a full data set change.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      adapter.dataChanges(scope)
 *          .consumeEach { /* handle data change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<T> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(this@dataChanges)
    val dataObserver = observer(scope, this@dataChanges, corbindEventEmitter())
    registerAdapterDataObserver(dataObserver)
    awaitClose { unregisterAdapterDataObserver(dataObserver) }
}

/**
 * Create a flow that emits [RecyclerView.Adapter] when `AdapterDataObserver.onChanged()` reports a
 * full data set change.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * adapter.dataChanges()
 *      .onEach { /* handle data change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * adapter.dataChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle data change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(): InitialValueFlow<T> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter<T>()
    val dataObserver = observer(this, this@dataChanges, emitter)
    registerAdapterDataObserver(dataObserver)
    emitter.sendInitialValue(this@dataChanges)
    awaitClose { unregisterAdapterDataObserver(dataObserver) }
}.asInitialValueFlow()

@CheckResult
private fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> observer(
    scope: CoroutineScope,
    adapter: T,
    emitter: (T) -> Boolean,
) = object : RecyclerView.AdapterDataObserver() {

    override fun onChanged() {
        if (scope.isActive) emitter(adapter)
    }
}

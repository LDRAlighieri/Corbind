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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on data change events for [RecyclerView.Adapter].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (T) -> Unit
) {
    val events = scope.actor<T>(Dispatchers.Main, capacity) {
        for (adapter in channel) action(adapter)
    }

    events.offer(this)
    val dataObserver = observer(scope, this, events::offer)
    registerAdapterDataObserver(dataObserver)
    events.invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}

/**
 * Perform an action on data change events for [RecyclerView.Adapter], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (T) -> Unit
) = coroutineScope {
    dataChanges(this, capacity, action)
}

/**
 * Create a channel of data change events for [RecyclerView.Adapter].
 *
 * *Note:* A value will be emitted immediately.
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
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<T> = corbindReceiveChannel(capacity) {
    safeOffer(this@dataChanges)
    val dataObserver = observer(scope, this@dataChanges, ::safeOffer)
    registerAdapterDataObserver(dataObserver)
    invokeOnClose { unregisterAdapterDataObserver(dataObserver) }
}

/**
 * Create a flow of data change events for [RecyclerView.Adapter].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * adapter.dataChanges()
 *      .onEach { /* handle data change */ }
 *      .launchIn(scope)
 *
 * // drop initial value
 * adapter.dataChanges()
 *      .drop(1)
 *      .onEach { /* handle data change */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> T.dataChanges(): Flow<T> = channelFlow {
    offer(this@dataChanges)
    val dataObserver = observer(this, this@dataChanges, ::offer)
    registerAdapterDataObserver(dataObserver)
    awaitClose { unregisterAdapterDataObserver(dataObserver) }
}

@CheckResult
private fun <T : RecyclerView.Adapter<out RecyclerView.ViewHolder>> observer(
    scope: CoroutineScope,
    adapter: T,
    emitter: (T) -> Boolean
) = object : RecyclerView.AdapterDataObserver() {

    override fun onChanged() {
        if (scope.isActive) { emitter(adapter) }
    }
}

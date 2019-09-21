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
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on the selected position of [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.OnItemSelectedListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (position in channel) action(position)
    }

    events.offer(selectedItemPosition)
    onItemSelectedListener = listener(scope, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

/**
 * Perform an action on the selected position of [AdapterView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.OnItemSelectedListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    itemSelections(this, capacity, action)
}

/**
 * Create a channel of the selected position of [AdapterView]. If nothing is selected,
 * [AdapterView.INVALID_POSITION] will be emitted
 *
 * *Warning:* The created channel uses [AdapterView.OnItemSelectedListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
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
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    offerElement(selectedItemPosition)
    onItemSelectedListener = listener(scope, ::offerElement)
    invokeOnClose { onItemSelectedListener = null }
}

/**
 * Create a flow of the selected position of [AdapterView]. If nothing is selected,
 * [AdapterView.INVALID_POSITION] will be emitted
 *
 * *Warning:* The created flow uses [AdapterView.OnItemSelectedListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * adapterView.itemSelections()
 *      .onEach { /* handle selected position */ }
 *      .launchIn(scope)
 *
 * // drop initial value
 * adapterView.itemSelections()
 *      .drop(1)
 *      .onEach { /* handle selected position */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemSelections(): Flow<Int> = channelFlow {
    offer(selectedItemPosition)
    onItemSelectedListener = listener(this, ::offer)
    awaitClose { onItemSelectedListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) { onEvent(position) }
    override fun onNothingSelected(parent: AdapterView<*>) { onEvent(AdapterView.INVALID_POSITION) }

    private fun onEvent(position: Int) {
        if (scope.isActive) { emitter(position) }
    }
}

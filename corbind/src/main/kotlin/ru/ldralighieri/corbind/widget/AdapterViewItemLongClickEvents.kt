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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.offerElement

data class AdapterViewItemLongClickEvent(
    val view: AdapterView<*>,
    val clickedView: View?,
    val position: Int,
    val id: Long
)

/**
 * Perform an action on [item long click events][AdapterViewItemLongClickEvent] for [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.OnItemLongClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
    action: suspend (AdapterViewItemLongClickEvent) -> Unit
) {
    val events = scope.actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(scope, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

/**
 * Perform an action on [item long click events][AdapterViewItemLongClickEvent] for [AdapterView],
 * inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.OnItemLongClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
    action: suspend (AdapterViewItemLongClickEvent) -> Unit
) = coroutineScope {
    itemLongClickEvents(this, capacity, handled, action)
}

/**
 * Create a channel of the [item long click events][AdapterViewItemLongClickEvent] for
 * [AdapterView].
 *
 * *Warning:* The created channel uses [AdapterView.OnItemLongClickListener]. Only one channel can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      adapterView.itemLongClickEvents(scope)
 *          .consumeEach { /* handle item long click event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<AdapterViewItemLongClickEvent> = corbindReceiveChannel(capacity) {
    onItemLongClickListener = listener(scope, handled, ::offerElement)
    invokeOnClose { onItemLongClickListener = null }
}

/**
 * Create a flow of the [item long click events][AdapterViewItemLongClickEvent] for [AdapterView].
 *
 * *Warning:* The created flow uses [AdapterView.OnItemLongClickListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * adapterView.itemLongClickEvents()
 *      .onEach { /* handle item long click event */ }
 *      .launchIn(scope)
 * ```
 *
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
    handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): Flow<AdapterViewItemLongClickEvent> = channelFlow {
    onItemLongClickListener = listener(this, handled, ::offer)
    awaitClose { onItemLongClickListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (AdapterViewItemLongClickEvent) -> Boolean,
    emitter: (AdapterViewItemLongClickEvent) -> Boolean
) = AdapterView.OnItemLongClickListener { parent, view: View?, position, id ->
    if (scope.isActive) {
        val event = AdapterViewItemLongClickEvent(parent, view, position, id)
        if (handled(event)) {
            emitter(event)
            return@OnItemLongClickListener true
        }
    }
    return@OnItemLongClickListener false
}

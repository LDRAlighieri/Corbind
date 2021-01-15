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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerCatching

data class AdapterViewItemClickEvent(
    val view: AdapterView<*>,
    val clickedView: View?,
    val position: Int,
    val id: Long
)

/**
 * Perform an action on the [item click events][AdapterViewItemClickEvent] for [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemClickEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AdapterViewItemClickEvent) -> Unit
) {
    val events = scope.actor<AdapterViewItemClickEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(scope, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

/**
 * Perform an action on the [item click events][AdapterViewItemClickEvent] for [AdapterView], inside
 * new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemClickEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AdapterViewItemClickEvent) -> Unit
) = coroutineScope {
    itemClickEvents(this, capacity, action)
}

/**
 * Create a channel of the [item click events][AdapterViewItemClickEvent] for [AdapterView].
 *
 * *Warning:* The created channel uses [AdapterView.setOnItemClickListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      adapterView.itemClickEvents(scope)
 *          .consumeEach { /* handle item click event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemClickEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AdapterViewItemClickEvent> = corbindReceiveChannel(capacity) {
    onItemClickListener = listener(scope, ::offerCatching)
    invokeOnClose { onItemClickListener = null }
}

/**
 * Create a flow of the [item click events][AdapterViewItemClickEvent] for [AdapterView].
 *
 * *Warning:* The created flow uses [AdapterView.setOnItemClickListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * adapterView.itemClickEvents()
 *      .onEach { /* handle item click event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemClickEvents(): Flow<AdapterViewItemClickEvent> = channelFlow {
    onItemClickListener = listener(this, ::offerCatching)
    awaitClose { onItemClickListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (AdapterViewItemClickEvent) -> Boolean
) = AdapterView.OnItemClickListener { parent, view: View?, position, id ->
    if (scope.isActive) {
        emitter(AdapterViewItemClickEvent(parent, view, position, id))
    }
}

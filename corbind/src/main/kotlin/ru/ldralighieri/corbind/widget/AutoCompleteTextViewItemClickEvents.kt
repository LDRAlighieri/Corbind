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
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
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
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on [item click events][AdapterViewItemClickEvent] on [AutoCompleteTextView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun AutoCompleteTextView.itemClickEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AdapterViewItemClickEvent) -> Unit
) {
    val events = scope.actor<AdapterViewItemClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(scope, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

/**
 * Perform an action on [item click events][AdapterViewItemClickEvent] on [AutoCompleteTextView],
 * inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun AutoCompleteTextView.itemClickEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AdapterViewItemClickEvent) -> Unit
) = coroutineScope {
    itemClickEvents(this, capacity, action)
}

/**
 * Create a channel of [item click events][AdapterViewItemClickEvent] on [AutoCompleteTextView].
 *
 * *Warning:* The created channel uses [AdapterView.setOnItemClickListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      autoCompleteTextView.itemClickEvents(scope)
 *          .consumeEach { /* handle item click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun AutoCompleteTextView.itemClickEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AdapterViewItemClickEvent> = corbindReceiveChannel(capacity) {
    onItemClickListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemClickListener = null }
}

/**
 * Create a flow of [item click events][AdapterViewItemClickEvent] on [AutoCompleteTextView].
 *
 * *Warning:* The created flow uses [AdapterView.setOnItemClickListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * autoCompleteTextView.itemClickEvents()
 *      .onEach { /* handle item click */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun AutoCompleteTextView.itemClickEvents(): Flow<AdapterViewItemClickEvent> = channelFlow {
    onItemClickListener = listener(this, ::offer)
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

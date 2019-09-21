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
import ru.ldralighieri.corbind.safeOffer

sealed class AdapterViewSelectionEvent {
    abstract val view: AdapterView<*>
}

data class AdapterViewItemSelectionEvent(
    override val view: AdapterView<*>,
    val selectedView: View?,
    val position: Int,
    val id: Long
) : AdapterViewSelectionEvent()

data class AdapterViewNothingSelectionEvent(
    override val view: AdapterView<*>
) : AdapterViewSelectionEvent()

/**
 * Perform an action on [selection events][AdapterViewSelectionEvent] for [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemSelectedListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.selectionEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AdapterViewSelectionEvent) -> Unit
) {
    val events = scope.actor<AdapterViewSelectionEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    onItemSelectedListener = listener(scope, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

/**
 * Perform an action on [selection events][AdapterViewSelectionEvent] for [AdapterView], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemSelectedListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.selectionEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AdapterViewSelectionEvent) -> Unit
) = coroutineScope {
    selectionEvents(this, capacity, action)
}

/**
 * Create a channel of [selection events][AdapterViewSelectionEvent] for [AdapterView].
 *
 * *Warning:* The created channel uses [AdapterView.setOnItemSelectedListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      adapterView.selectionEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is AdapterViewItemSelectionEvent -> { /* handle item selection event */ }
 *                  is AdapterViewNothingSelectionEvent -> { /* handle nothing selection event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      adapterView.selectionEvents(scope)
 *          .filterIsInstance<AdapterViewItemSelectionEvent>()
 *          .consumeEach { /* handle item selection event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.selectionEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AdapterViewSelectionEvent> = corbindReceiveChannel(capacity) {
    safeOffer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemSelectedListener = null }
}

/**
 * Create a flow of [selection events][AdapterViewSelectionEvent] for [AdapterView].
 *
 * *Warning:* The created flow uses [AdapterView.setOnItemSelectedListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * adapterView.selectionEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is AdapterViewItemSelectionEvent -> { /* handle item selection event */ }
 *              is AdapterViewNothingSelectionEvent -> { /* handle nothing selection event */ }
 *          }
 *      }
 *      .launchIn(scope)
 *
 * // handle one event
 * adapterView.selectionEvents()
 *      .filterIsInstance<AdapterViewItemSelectionEvent>()
 *      .onEach { /* handle item selection event */ }
 *      .launchIn(scope)
 *
 * // drop initial value
 * adapterView.selectionEvents()
 *      .drop(1)
 *      .onEach { /* handle selection event */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.selectionEvents(): Flow<AdapterViewSelectionEvent> = channelFlow {
    offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(this, ::offer)
    awaitClose { onItemSelectedListener = null }
}

@CheckResult
private fun <T : Adapter> initialValue(adapterView: AdapterView<T>): AdapterViewSelectionEvent {
    return if (adapterView.selectedItemPosition == AdapterView.INVALID_POSITION) {
        AdapterViewNothingSelectionEvent(adapterView)
    } else {
        AdapterViewItemSelectionEvent(adapterView, adapterView.selectedView,
                adapterView.selectedItemPosition, adapterView.selectedItemId)
    }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (AdapterViewSelectionEvent) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        onEvent(AdapterViewItemSelectionEvent(parent, view, position, id))
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        onEvent(AdapterViewNothingSelectionEvent(parent))
    }

    private fun onEvent(event: AdapterViewSelectionEvent) {
        if (scope.isActive) { emitter(event) }
    }
}

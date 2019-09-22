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

/**
 * Perform an action on the position of item clicks for [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (position in channel) action(position)
    }

    onItemClickListener = listener(scope, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

/**
 * Perform an action on the position of item clicks for [AdapterView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    itemClicks(this, capacity, action)
}

/**
 * Create a channel of the position of item clicks for [AdapterView].
 *
 * *Warning:* The created channel uses [AdapterView.setOnItemClickListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      adapterView.itemClicks(scope)
 *          .consumeEach { /* handle item click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    onItemClickListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemClickListener = null }
}

/**
 * Create a flow of the position of item clicks for [AdapterView].
 *
 * *Warning:* The created flow uses [AdapterView.setOnItemClickListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * adapterView.itemClicks()
 *      .onEach { /* handle item click */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemClicks(): Flow<Int> = channelFlow {
    onItemClickListener = listener(this, ::offer)
    awaitClose { onItemClickListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = AdapterView.OnItemClickListener { _, _: View?, position, _ ->
    if (scope.isActive) { emitter(position) }
}

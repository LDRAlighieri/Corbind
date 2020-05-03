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
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on position of item long clicks for [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemLongClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 * @param action An action to perform
 */
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (position in channel) action(position)
    }

    onItemLongClickListener = listener(scope, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

/**
 * Perform an action on position of item long clicks for [AdapterView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemLongClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 * @param action An action to perform
 */
suspend fun <T : Adapter> AdapterView<T>.itemLongClicks(
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend (Int) -> Unit
) = coroutineScope {
    itemLongClicks(this, capacity, handled, action)
}

/**
 * Create a channel of the position of item long clicks for [AdapterView].
 *
 * *Warning:* The created channel uses [AdapterView.setOnItemLongClickListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      view.hovers(scope)
 *          .consumeEach { /* handle item long click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    onItemLongClickListener = listener(scope, handled, ::safeOffer)
    invokeOnClose { onItemLongClickListener = null }
}

/**
 * Create a flow of the position of item long clicks for [AdapterView].
 *
 * *Warning:* The created flow uses [AdapterView.setOnItemLongClickListener]. Only one flow can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * adapterView.itemLongClicks()
 *      .onEach { /* handle item long click */ }
 *      .launchIn(scope)
 * ```
 *
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    handled: () -> Boolean = AlwaysTrue
): Flow<Int> = channelFlow {
    onItemLongClickListener = listener(this, handled, ::offer)
    awaitClose { onItemLongClickListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: () -> Boolean,
    emitter: (Int) -> Boolean
) = AdapterView.OnItemLongClickListener { _, _: View?, position, _ ->
    if (scope.isActive && handled()) {
        emitter(position)
        return@OnItemLongClickListener true
    }
    return@OnItemLongClickListener false
}

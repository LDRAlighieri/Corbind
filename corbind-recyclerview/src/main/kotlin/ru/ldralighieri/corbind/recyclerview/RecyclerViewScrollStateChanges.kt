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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

/**
 * Perform an action on scroll state changes on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RecyclerView.scrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (state in channel) action(state)
    }

    val scrollListener = listener(scope, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}

/**
 * Perform an action on scroll state changes on [RecyclerView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RecyclerView.scrollStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, capacity) {
        for (state in channel) action(state)
    }

    val scrollListener = listener(this, events::offer)
    addOnScrollListener(scrollListener)
    events.invokeOnClose { removeOnScrollListener(scrollListener) }
}

/**
 * Create a channel of scroll state changes on [RecyclerView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RecyclerView.scrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val scrollListener = listener(scope, ::safeOffer)
    addOnScrollListener(scrollListener)
    invokeOnClose { removeOnScrollListener(scrollListener) }
}

/**
 * Create a flow of scroll state changes on [RecyclerView].
 */
@CheckResult
fun RecyclerView.scrollStateChanges(): Flow<Int> = channelFlow {
    val scrollListener = listener(this, ::offer)
    addOnScrollListener(scrollListener)
    awaitClose { removeOnScrollListener(scrollListener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : RecyclerView.OnScrollListener() {

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (scope.isActive) { emitter(newState) }
    }
}

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

package ru.ldralighieri.corbind.swiperefreshlayout

import androidx.annotation.CheckResult
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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

/**
 * Perform an action on refresh events on [SwipeRefreshLayout].
 *
 * *Warning:* The created actor uses [SwipeRefreshLayout.setOnRefreshListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SwipeRefreshLayout.refreshes(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setOnRefreshListener(listener(scope, events::offer))
    events.invokeOnClose { setOnRefreshListener(null) }
}

/**
 * Perform an action on refresh events on [SwipeRefreshLayout], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [SwipeRefreshLayout.setOnRefreshListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SwipeRefreshLayout.refreshes(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    refreshes(this, capacity, action)
}

/**
 * Create a channel of refresh events on [SwipeRefreshLayout].
 *
 * *Warning:* The created channel uses [SwipeRefreshLayout.setOnRefreshListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      swipeRefreshLayout.refreshes(scope)
 *          .consumeEach { /* handle refresh */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SwipeRefreshLayout.refreshes(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnRefreshListener(listener(scope, ::offerCatching))
    invokeOnClose { setOnRefreshListener(null) }
}

/**
 * Create a flow of refresh events on [SwipeRefreshLayout].
 *
 * *Warning:* The created flow uses [SwipeRefreshLayout.setOnRefreshListener]. Only one flow can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * swipeRefreshLayout.refreshes()
 *      .onEach { /* handle refresh */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SwipeRefreshLayout.refreshes(): Flow<Unit> = channelFlow<Unit> {
    setOnRefreshListener(listener(this, ::offerCatching))
    awaitClose { setOnRefreshListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = SwipeRefreshLayout.OnRefreshListener {
    if (scope.isActive) { emitter(Unit) }
}

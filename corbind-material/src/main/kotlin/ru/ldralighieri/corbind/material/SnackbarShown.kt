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

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.snackbar.Snackbar
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
 * Perform an action on the show events from [Snackbar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Snackbar.shown(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Snackbar) -> Unit
) {
    val events = scope.actor<Snackbar>(Dispatchers.Main, capacity) {
        for (snackbar in channel) action(snackbar)
    }

    val callback = callback(scope, events::offer)
    addCallback(callback)
    events.invokeOnClose { removeCallback(callback) }
}

/**
 * Perform an action on the show events from [Snackbar], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Snackbar.shown(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Snackbar) -> Unit
) = coroutineScope {
    shown(this, capacity, action)
}

/**
 * Create a channel which emits the show events from [Snackbar].
 *
 * Example:
 *
 * ```
 * launch {
 *      snackbar.shown(scope)
 *          .consumeEach { /* handle show */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Snackbar.shown(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Snackbar> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, ::offerElement)
    addCallback(callback)
    invokeOnClose { removeCallback(callback) }
}

/**
 * Create a flow which emits the show events from [Snackbar].
 *
 * Example:
 *
 * ```
 * snackbar.shown()
 *      .onEach { /* handle show */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun Snackbar.shown(): Flow<Snackbar> = channelFlow {
    val callback = callback(this, ::offer)
    addCallback(callback)
    awaitClose { removeCallback(callback) }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Snackbar) -> Boolean
) = object : Snackbar.Callback() {
    override fun onShown(sb: Snackbar) {
        if (scope.isActive) { emitter(sb) }
    }
}

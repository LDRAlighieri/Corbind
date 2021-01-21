/*
 * Copyright 2021 Vladimir Raupov
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

package ru.ldralighieri.corbind.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

/**
 * Perform an action when the the Intent broadcasts by the selected filter.
 *
 * @param scope Root coroutine scope
 * @param intentFilter Selects the Intent broadcasts to be received
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Context.receivesBroadcast(
    scope: CoroutineScope,
    intentFilter: IntentFilter,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Intent) -> Unit
) {
    val events = scope.actor<Intent>(Dispatchers.Main.immediate, capacity) {
        for (intent in channel) action(intent)
    }

    val receiver = receiver(scope, events::offer)
    registerReceiver(receiver, intentFilter)
    events.invokeOnClose { unregisterReceiver(receiver) }
}

/**
 * Perform an action when the the Intent broadcasts by the selected filter, inside new
 * [CoroutineScope].
 *
 * @param intentFilter Selects the Intent broadcasts to be received
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Context.receivesBroadcast(
    intentFilter: IntentFilter,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Intent) -> Unit
) = coroutineScope {
    receivesBroadcast(this, intentFilter, capacity, action)
}

/**
 * Create a channel which emits the Intent broadcasts by the selected filter.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialDatePicker
 *          .receivesBroadcast(
 *              scope,
 *              IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
 *          )
 *          .consumeEach { /* handle nfc adapter state changed */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param intentFilter Selects the Intent broadcasts to be received
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Context.receivesBroadcast(
    scope: CoroutineScope,
    intentFilter: IntentFilter,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Intent> = corbindReceiveChannel(capacity) {
    val receiver = receiver(scope, ::offerCatching)
    registerReceiver(receiver, intentFilter)
    invokeOnClose { unregisterReceiver(receiver) }
}

/**
 * Create a flow which emits the Intent broadcasts by the selected filter.
 *
 * Example:
 *
 * ```
 * context
 *      .receivesBroadcast(
 *          IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
 *      )
 *      .onEach { /* handle nfc adapter state changed */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param intentFilter Selects the Intent broadcasts to be received
 */
fun Context.receivesBroadcast(intentFilter: IntentFilter): Flow<Intent> = channelFlow {
    val receiver = receiver(this, ::offerCatching)
    registerReceiver(receiver, intentFilter)
    awaitClose { unregisterReceiver(receiver) }
}

@CheckResult
private fun receiver(
    scope: CoroutineScope,
    emitter: (Intent) -> Boolean
) = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (scope.isActive) { emitter(intent) }
    }
}

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

@file:SuppressWarnings("UnspecifiedRegisterReceiverFlag")

package ru.ldralighieri.corbind.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action when a broadcast intent matches [intentFilter].
 *
 * @param scope Coroutine scope that owns the binding
 * @param intentFilter Selects which broadcast intents to receive
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun Context.receivesBroadcast(
    scope: CoroutineScope,
    intentFilter: IntentFilter,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Intent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Intent>(Dispatchers.Main.immediate, capacity) {
        for (intent in channel) action(intent)
    }

    val receiver = receiver(scope, events.corbindEventEmitter(scope))

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter)
    } else {
        registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    }

    events.invokeOnCloseOnMain { unregisterReceiver(receiver) }
}

/**
 * Perform an action when a broadcast intent matches [intentFilter], in a new
 * [CoroutineScope].
 *
 * @param intentFilter Selects which broadcast intents to receive
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun Context.receivesBroadcast(
    intentFilter: IntentFilter,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Intent) -> Unit,
) = coroutineScope {
    receivesBroadcast(this, intentFilter, capacity, action)
}

/**
 * Create a channel that emits broadcast intents matching [intentFilter].
 *
 * Example:
 *
 * ```
 * launch {
 *      context
 *          .receivesBroadcast(
 *              scope,
 *              IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
 *          )
 *          .consumeEach { /* handle nfc adapter state changed */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param intentFilter Selects which broadcast intents to receive
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun Context.receivesBroadcast(
    scope: CoroutineScope,
    intentFilter: IntentFilter,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Intent> = corbindReceiveChannel(scope, capacity) {
    val receiver = receiver(scope, corbindEventEmitter())

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter)
    } else {
        registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    }

    awaitClose { unregisterReceiver(receiver) }
}

/**
 * Create a flow that emits broadcast intents matching [intentFilter].
 *
 * Example:
 *
 * ```
 * context
 *      .receivesBroadcast(
 *          IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
 *      )
 *      .onEach { /* handle nfc adapter state changed */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param intentFilter Selects which broadcast intents to receive
 */
fun Context.receivesBroadcast(intentFilter: IntentFilter): Flow<Intent> = corbindCallbackFlow {
    val receiver = receiver(this, corbindEventEmitter())

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter)
    } else {
        registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    }

    awaitClose { unregisterReceiver(receiver) }
}

@CheckResult
private fun receiver(
    scope: CoroutineScope,
    emitter: (Intent) -> Boolean,
) = object : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (scope.isActive) emitter(intent)
    }
}

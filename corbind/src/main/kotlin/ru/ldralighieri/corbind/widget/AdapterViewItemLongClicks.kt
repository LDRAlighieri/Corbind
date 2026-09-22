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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action on position of item long clicks for [AdapterView].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemLongClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (position in channel) action(position)
    }

    onItemLongClickListener = listener(scope, handled, events.corbindEventEmitter(scope))
    events.invokeOnCloseOnMain { onItemLongClickListener = null }
}

/**
 * Perform an action on position of item long clicks for [AdapterView], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AdapterView.setOnItemLongClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
suspend fun <T : Adapter> AdapterView<T>.itemLongClicks(
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend (Int) -> Unit,
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
 *      adapterView.itemLongClicks(scope)
 *          .consumeEach { /* handle item long click */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    onItemLongClickListener = listener(scope, handled, corbindEventEmitter())
    awaitClose { onItemLongClickListener = null }
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
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Function invoked each occurrence to determine the return value of the underlying
 * [AdapterView.OnItemLongClickListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
    handled: () -> Boolean = AlwaysTrue,
): Flow<Int> = corbindCallbackFlow {
    onItemLongClickListener = listener(this, handled, corbindEventEmitter())
    awaitClose { onItemLongClickListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: () -> Boolean,
    emitter: (Int) -> Boolean,
) = AdapterView.OnItemLongClickListener { _, _: View?, position, _ ->
    if (scope.isActive && handled()) {
        return@OnItemLongClickListener emitter(position)
    }
    return@OnItemLongClickListener false
}

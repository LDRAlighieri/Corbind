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

package ru.ldralighieri.corbind.view

import android.view.MenuItem
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
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

/**
 * Perform an action on [MenuItem] click events.
 *
 * *Warning:* The created actor uses [MenuItem.setOnMenuItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 * @param action An action to perform
 */
fun MenuItem.clicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItem) -> Boolean = AlwaysTrue,
    action: suspend (MenuItem) -> Unit,
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main.immediate, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, handled, events::trySend))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Perform an action on [MenuItem] click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [MenuItem.setOnMenuItemClickListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 * @param action An action to perform
 */
suspend fun MenuItem.clicks(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItem) -> Boolean = AlwaysTrue,
    action: suspend (MenuItem) -> Unit,
) = coroutineScope {
    clicks(this, capacity, handled, action)
}

/**
 * Create a channel which emits on [MenuItem] click events.
 *
 * *Warning:* The created channel uses [MenuItem.setOnMenuItemClickListener]. Only one channel can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      datePickerDialog.dateSetEvents(scope)
 *          .consumeEach { /* handle click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 */
@CheckResult
fun MenuItem.clicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItem) -> Boolean = AlwaysTrue,
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, handled, ::trySend))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Create a flow which emits on [MenuItem] click events.
 *
 * *Warning:* The created flow uses [MenuItem.setOnMenuItemClickListener]. Only one flow can be used
 * at a time.
 *
 * Example:
 *
 * ```
 * menuItem.clicks()
 *      .onEach { /* handle click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 */
@CheckResult
fun MenuItem.clicks(
    handled: (MenuItem) -> Boolean = AlwaysTrue,
): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, handled, ::trySend))
    awaitClose { setOnMenuItemClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MenuItem) -> Boolean,
    emitter: (MenuItem) -> Unit,
) = MenuItem.OnMenuItemClickListener { item ->
    if (scope.isActive && handled(item)) {
        emitter(item)
        return@OnMenuItemClickListener true
    }
    return@OnMenuItemClickListener false
}

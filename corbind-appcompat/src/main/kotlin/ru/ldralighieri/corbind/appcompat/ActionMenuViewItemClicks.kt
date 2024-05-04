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

package ru.ldralighieri.corbind.appcompat

import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.appcompat.widget.ActionMenuView
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

/**
 * Perform an action on clicked menu item in [ActionMenuView].
 *
 * *Warning:* The created actor uses [ActionMenuView.setOnMenuItemClickListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ActionMenuView.itemClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main.immediate, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Perform an action on clicked menu item in [ActionMenuView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [ActionMenuView.setOnMenuItemClickListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ActionMenuView.itemClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) = coroutineScope {
    itemClicks(this, capacity, action)
}

/**
 * Create a channel which emits the clicked menu item in [ActionMenuView].
 *
 * *Warning:* The created channel uses [ActionMenuView.setOnMenuItemClickListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      actionMenuView.itemClicks(scope)
 *          .consumeEach { /* handle menu item */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ActionMenuView.itemClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, ::trySend))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Create a flow which emits the clicked menu item in [ActionMenuView].
 *
 * *Warning:* The created flow uses [ActionMenuView.setOnMenuItemClickListener]. Only one flow can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * actionMenuView.itemClicks()
 *      .onEach { /* handle menu item */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun ActionMenuView.itemClicks(): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, ::trySend))
    awaitClose { setOnMenuItemClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MenuItem) -> Unit,
) = ActionMenuView.OnMenuItemClickListener {
    if (scope.isActive) emitter(it)
    return@OnMenuItemClickListener true
}

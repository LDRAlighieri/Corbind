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

import android.view.MenuItem
import androidx.annotation.CheckResult
import com.google.android.material.navigation.NavigationBarView
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
 * Perform an action on the reselected item in [NavigationBarView].
 *
 * *Warning:* The created actor uses [NavigationBarView.setOnItemReselectedListener]. Only one actor
 * can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun NavigationBarView.itemReselections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main.immediate, capacity) {
        for (item in channel) action(item)
    }

    setOnItemReselectedListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnItemReselectedListener(null) }
}

/**
 * Perform an action on the reselected item in [NavigationBarView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [NavigationBarView.setOnItemReselectedListener]. Only one actor
 * can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun NavigationBarView.itemReselections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) = coroutineScope {
    itemReselections(this, capacity, action)
}

/**
 * Create a channel which emits the reselected item in [NavigationBarView].
 *
 * *Warning:* The created channel uses [NavigationBarView.setOnItemReselectedListener]. Only one
 * channel can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      anyNavigationBarView.itemReselections(scope)
 *          .consumeEach { /* handle reselected item */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NavigationBarView.itemReselections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnItemReselectedListener(listener(scope, ::trySend))
    invokeOnClose { setOnItemReselectedListener(null) }
}

/**
 * Create a flow which emits the reselected item in [NavigationBarView].
 *
 * *Warning:* The created flow uses [NavigationBarView.setOnItemReselectedListener]. Only one flow
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * anyNavigationBarView.itemReselections()
 *      .onEach { /* handle reselected item */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun NavigationBarView.itemReselections(): Flow<MenuItem> = channelFlow {
    setOnItemReselectedListener(listener(this, ::trySend))
    awaitClose { setOnItemReselectedListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MenuItem) -> Unit,
) = NavigationBarView.OnItemReselectedListener {
    if (scope.isActive) emitter(it)
}

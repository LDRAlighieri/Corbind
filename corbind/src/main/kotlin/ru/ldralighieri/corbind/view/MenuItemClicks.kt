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
import ru.ldralighieri.corbind.internal.offerElement

/**
 * Perform an action on [MenuItem] click events.
 *
 * *Warning:* The created actor uses [MenuItem.setOnMenuItemClickListener] to emmit clicks. Only
 * one actor can be used for a menu item at a time.
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
    action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Perform an action on [MenuItem] click events inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [MenuItem.setOnMenuItemClickListener] to emmit clicks. Only
 * one actor can be used for a menu item at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 * @param action An action to perform
 */
suspend fun MenuItem.clicks(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItem) -> Boolean = AlwaysTrue,
    action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Create a channel which emits on [MenuItem] click events.
 *
 * *Warning:* The created channel uses [MenuItem.setOnMenuItemClickListener] to emmit clicks.
 * Only one channel can be used for a menu item at a time.
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
    handled: (MenuItem) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setOnMenuItemClickListener(listener(scope, handled, ::offerElement))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

/**
 * Create a flow which emits on [MenuItem] click events.
 *
 * *Warning:* The created flow uses [MenuItem.setOnMenuItemClickListener] to emmit clicks. Only
 * one flow can be used for a menu item at a time.
 *
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnMenuItemClickListener]
 */
@CheckResult
fun MenuItem.clicks(
    handled: (MenuItem) -> Boolean = AlwaysTrue
): Flow<MenuItem> = channelFlow {
    setOnMenuItemClickListener(listener(this, handled, ::offer))
    awaitClose { setOnMenuItemClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MenuItem) -> Boolean,
    emitter: (MenuItem) -> Boolean
) = MenuItem.OnMenuItemClickListener { item ->

    if (scope.isActive) {
        if (handled(item)) {
            emitter(item)
            return@OnMenuItemClickListener true
        }
    }

    return@OnMenuItemClickListener false
}

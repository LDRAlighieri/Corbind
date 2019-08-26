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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

sealed class MenuItemActionViewEvent {
    abstract val menuItem: MenuItem
}

data class MenuItemActionViewCollapseEvent(
    override val menuItem: MenuItem
) : MenuItemActionViewEvent()

data class MenuItemActionViewExpandEvent(
    override val menuItem: MenuItem
) : MenuItemActionViewEvent()

/**
 * Perform an action on action view events for [MenuItem].
 *
 * *Warning:* The created actor uses [MenuItem.setOnActionExpandListener] to emit action view
 * events. Only one actor can be used for a menu item at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]
 * @param action An action to perform
 */
fun MenuItem.actionViewEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
    action: suspend (MenuItemActionViewEvent) -> Boolean
) {

    val events = scope.actor<MenuItemActionViewEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}

/**
 * Perform an action on action view events for [MenuItem] inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [MenuItem.setOnActionExpandListener] to emit action view
 * events. Only one actor can be used for a menu item at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]
 * @param action An action to perform
 */
suspend fun MenuItem.actionViewEvents(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
    action: suspend (MenuItemActionViewEvent) -> Unit
) = coroutineScope {

    val events = actor<MenuItemActionViewEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}

/**
 * Create a channel of action view events for [MenuItem].
 *
 * *Warning:* The created channel uses [MenuItem.setOnActionExpandListener] to emit action view
 * events. Only one channel can be used for a menu item at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]
 */
@CheckResult
fun MenuItem.actionViewEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItemActionViewEvent> = corbindReceiveChannel(capacity) {
    setOnActionExpandListener(listener(scope, handled, ::offerElement))
    invokeOnClose { setOnActionExpandListener(null) }
}

/**
 * Create a flow of action view events for [MenuItem].
 *
 * *Warning:* The created flow uses [MenuItem.setOnActionExpandListener] to emit action view
 * events. Only one flow can be used for a menu item at a time.
 *
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]
 */
@CheckResult
fun MenuItem.actionViewEvents(
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): Flow<MenuItemActionViewEvent> = channelFlow {
    setOnActionExpandListener(listener(this, handled, ::offer))
    awaitClose { setOnActionExpandListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MenuItemActionViewEvent) -> Boolean,
    emitter: (MenuItemActionViewEvent) -> Boolean
) = object : MenuItem.OnActionExpandListener {

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewExpandEvent(item))
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewCollapseEvent(item))
    }

    private fun onEvent(event: MenuItemActionViewEvent): Boolean {
        if (scope.isActive) {
            if (handled(event)) {
                emitter(event)
                return true
            }
        }
        return false
    }
}

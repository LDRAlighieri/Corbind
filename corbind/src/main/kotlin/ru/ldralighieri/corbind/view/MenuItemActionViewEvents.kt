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

sealed interface MenuItemActionViewEvent {
    val menuItem: MenuItem
}

data class MenuItemActionViewCollapseEvent(
    override val menuItem: MenuItem,
) : MenuItemActionViewEvent

data class MenuItemActionViewExpandEvent(
    override val menuItem: MenuItem,
) : MenuItemActionViewEvent

/**
 * Perform an action on [action view events][MenuItemActionViewEvent] for [MenuItem].
 *
 * *Warning:* The created actor uses [MenuItem.setOnActionExpandListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
fun MenuItem.actionViewEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
    action: suspend (MenuItemActionViewEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<MenuItemActionViewEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(scope, handled, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnActionExpandListener(null) }
}

/**
 * Perform an action on [action view events][MenuItemActionViewEvent] for [MenuItem], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [MenuItem.setOnActionExpandListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 * @param action An action to perform
 */
@MainThread
suspend fun MenuItem.actionViewEvents(
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
    action: suspend (MenuItemActionViewEvent) -> Unit,
) = coroutineScope {
    actionViewEvents(this, capacity, handled, action)
}

/**
 * Create a channel of [action view events][MenuItemActionViewEvent] for [MenuItem].
 *
 * *Warning:* The created channel uses [MenuItem.setOnActionExpandListener]. Only one channel can be
 * used at a time.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      menuItem.actionViewEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is MenuItemActionViewCollapseEvent -> { /* handle collapse event */ }
 *                  is MenuItemActionViewExpandEvent -> { /* handle expand event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      menuItem.actionViewEvents(scope)
 *          .filterIsInstance<MenuItemActionViewCollapseEvent>()
 *          .consumeEach { /* handle collapse event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun MenuItem.actionViewEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
): ReceiveChannel<MenuItemActionViewEvent> = corbindReceiveChannel(scope, capacity) {
    setOnActionExpandListener(listener(scope, handled, corbindEventEmitter()))
    awaitClose { setOnActionExpandListener(null) }
}

/**
 * Create a flow of [action view events][MenuItemActionViewEvent] for [MenuItem].
 *
 * *Warning:* The created flow uses [MenuItem.setOnActionExpandListener]. Only one flow can be used
 * at a time.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * menuItem.actionViewEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is MenuItemActionViewCollapseEvent -> { /* handle collapse event */ }
 *              is MenuItemActionViewExpandEvent -> { /* handle expand event */ }
 *          }
 *      }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // handle one event
 * menuItem.actionViewEvents()
 *      .filterIsInstance<MenuItemActionViewCollapseEvent>()
 *      .onEach { /* handle collapse event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 *
 * @param handled Function invoked with each value to determine the return value of the underlying
 * [MenuItem.OnActionExpandListener]. The listener handles the event only when it is also accepted by the configured delivery policy.
 */
@CheckResult
fun MenuItem.actionViewEvents(
    handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
): Flow<MenuItemActionViewEvent> = corbindCallbackFlow {
    setOnActionExpandListener(listener(this, handled, corbindEventEmitter()))
    awaitClose { setOnActionExpandListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: (MenuItemActionViewEvent) -> Boolean,
    emitter: (MenuItemActionViewEvent) -> Boolean,
) = object : MenuItem.OnActionExpandListener {

    override fun onMenuItemActionExpand(item: MenuItem): Boolean = onEvent(MenuItemActionViewExpandEvent(item))

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean = onEvent(MenuItemActionViewCollapseEvent(item))

    private fun onEvent(event: MenuItemActionViewEvent): Boolean {
        if (scope.isActive && handled(event)) {
            return emitter(event)
        }
        return false
    }
}

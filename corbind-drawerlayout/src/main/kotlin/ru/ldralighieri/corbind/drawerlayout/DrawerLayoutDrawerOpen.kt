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

package ru.ldralighieri.corbind.drawerlayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.drawerlayout.widget.DrawerLayout
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
 * Perform an action on the open state of the [DrawerLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param gravity Gravity of the drawer to check
 * @param action An action to perform
 */
fun DrawerLayout.drawerOpens(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    gravity: Int,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, capacity) {
        for (open in channel) action(open)
    }

    events.offer(isDrawerOpen(gravity))
    val listener = listener(scope, gravity, events::offer)
    addDrawerListener(listener)
    events.invokeOnClose { removeDrawerListener(listener) }
}

/**
 * Perform an action on the open state of the [DrawerLayout], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param gravity Gravity of the drawer to check
 * @param action An action to perform
 */
suspend fun DrawerLayout.drawerOpens(
    capacity: Int = Channel.RENDEZVOUS,
    gravity: Int,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    drawerOpens(this, capacity, gravity, action)
}

/**
 * Create a channel of the open state of the [DrawerLayout].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      drawerLayout.drawerOpens(scope)
 *          .consumeEach { /* handle open state */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param gravity Gravity of the drawer to check
 */
@CheckResult
fun DrawerLayout.drawerOpens(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    gravity: Int
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    offerElement(isDrawerOpen(gravity))
    val listener = listener(scope, gravity, ::offerElement)
    addDrawerListener(listener)
    invokeOnClose { removeDrawerListener(listener) }
}

/**
 * Create a flow of the open state of the [DrawerLayout].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * drawerLayout.drawerOpens()
 *      .onEach { /* handle open state */ }
 *      .launchIn(scope)
 *
 * // drop initial value
 * adapter.dataChanges()
 *      .drop(1)
 *      .onEach { /* handle open state */ }
 *      .launchIn(scope)
 * ```
 *
 * @param gravity Gravity of the drawer to check
 */
@CheckResult
fun DrawerLayout.drawerOpens(
    gravity: Int
): Flow<Boolean> = channelFlow {
    offer(isDrawerOpen(gravity))
    val listener = listener(this, gravity, ::offer)
    addDrawerListener(listener)
    awaitClose { removeDrawerListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    gravity: Int,
    emitter: (Boolean) -> Boolean
) = object : DrawerLayout.DrawerListener {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) { }
    override fun onDrawerOpened(drawerView: View) { onEvent(drawerView, true) }
    override fun onDrawerClosed(drawerView: View) { onEvent(drawerView, false) }
    override fun onDrawerStateChanged(newState: Int) { }

    private fun onEvent(drawerView: View, opened: Boolean) {
        if (scope.isActive) {
            val drawerGravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
            if (drawerGravity == gravity) { emitter(opened) }
        }
    }
}

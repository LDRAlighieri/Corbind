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
import com.google.android.material.navigation.NavigationView
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
 * Perform an action on the selected item in [NavigationView].
 *
 * *Warning:* The created actor uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * actor can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun NavigationView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main.immediate, capacity) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events::offer)
    setNavigationItemSelectedListener(listener(scope, events::offer))
    events.invokeOnClose { setNavigationItemSelectedListener(null) }
}

/**
 * Perform an action on the selected item in [NavigationView], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * actor can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun NavigationView.itemSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit
) = coroutineScope {
    itemSelections(this, capacity, action)
}

/**
 * Create a channel which emits the selected item in [NavigationView].
 *
 * *Warning:* The created channel uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * channel can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      navigationView.itemSelections(scope)
 *          .consumeEach { /* handle selected item */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NavigationView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {
    setInitialValue(this@itemSelections, ::offerCatching)
    setNavigationItemSelectedListener(listener(scope, ::offerCatching))
    invokeOnClose { setNavigationItemSelectedListener(null) }
}

/**
 * Create a flow which emits the selected item in [NavigationView].
 *
 * *Warning:* The created flow uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * flow can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * navigationView.itemSelections()
 *      .onEach { /* handle selected item */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * navigationView.itemSelections()
 *      .drop(1)
 *      .onEach { /* handle selected item */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun NavigationView.itemSelections(): Flow<MenuItem> = channelFlow<MenuItem> {
    setInitialValue(this@itemSelections, ::offerCatching)
    setNavigationItemSelectedListener(listener(this, ::offerCatching))
    awaitClose { setNavigationItemSelectedListener(null) }
}

private fun setInitialValue(
    navigationView: NavigationView,
    emitter: (MenuItem) -> Boolean
) {
    val menu = navigationView.menu
    for (i in 0 until menu.size()) {
        val item = menu.getItem(i)
        if (item.isChecked) {
            emitter(item)
            break
        }
    }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MenuItem) -> Boolean
) = NavigationView.OnNavigationItemSelectedListener {
    if (scope.isActive) { emitter(it) }
    return@OnNavigationItemSelectedListener true
}

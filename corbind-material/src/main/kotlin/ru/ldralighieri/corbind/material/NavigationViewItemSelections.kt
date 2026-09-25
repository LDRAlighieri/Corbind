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
import androidx.annotation.MainThread
import androidx.core.view.iterator
import com.google.android.material.navigation.NavigationView
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
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on the selected item in [NavigationView].
 *
 * *Warning:* The created actor uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * actor can be used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun NavigationView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<MenuItem>(Dispatchers.Main.immediate, capacity) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events.corbindEventEmitter(scope))
    setNavigationItemSelectedListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setNavigationItemSelectedListener(null) }
}

/**
 * Perform an action on the selected item in [NavigationView], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * actor can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun NavigationView.itemSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) = coroutineScope {
    itemSelections(this, capacity, action)
}

/**
 * Create a channel which emits the selected item in [NavigationView].
 *
 * *Warning:* The created channel uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * channel can be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun NavigationView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<MenuItem> = corbindReceiveChannel(scope, capacity) {
    setInitialValue(this@itemSelections) {
        sendInitialValue(it)
        true
    }
    setNavigationItemSelectedListener(listener(scope, corbindEventEmitter()))
    awaitClose { setNavigationItemSelectedListener(null) }
}

/**
 * Create a flow which emits the selected item in [NavigationView].
 *
 * *Warning:* The created flow uses [NavigationView.setNavigationItemSelectedListener]. Only one
 * flow can be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * navigationView.itemSelections()
 *      .onEach { /* handle selected item */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * navigationView.itemSelections()
 *      .drop(1)
 *      .onEach { /* handle selected item */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun NavigationView.itemSelections(): Flow<MenuItem> = corbindCallbackFlow {
    setInitialValue(this@itemSelections, corbindEventEmitter())
    setNavigationItemSelectedListener(listener(this, corbindEventEmitter()))
    awaitClose { setNavigationItemSelectedListener(null) }
}

private fun setInitialValue(
    navigationView: NavigationView,
    emitter: (MenuItem) -> Boolean,
) {
    for (item in navigationView.menu) {
        if (item.isChecked) {
            emitter(item)
            break
        }
    }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (MenuItem) -> Boolean,
) = NavigationView.OnNavigationItemSelectedListener {
    if (scope.isActive) {
        return@OnNavigationItemSelectedListener emitter(it)
    }
    return@OnNavigationItemSelectedListener false
}

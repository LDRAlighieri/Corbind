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
import com.google.android.material.navigation.NavigationBarView
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
 * Perform an action on the selected item in [NavigationBarView].
 *
 * *Warning:* The created actor uses [NavigationBarView.setOnItemSelectedListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun NavigationBarView.itemSelections(
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
    setOnItemSelectedListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnItemSelectedListener(null) }
}

/**
 * Perform an action on the selected item in [NavigationBarView], in a new [CoroutineScope].
 *
 * *Warning:* The created actor uses [NavigationBarView.setOnItemSelectedListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun NavigationBarView.itemSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (MenuItem) -> Unit,
) = coroutineScope {
    itemSelections(this, capacity, action)
}

/**
 * Create a channel which emits the selected item in [NavigationBarView].
 *
 * *Warning:* The created channel uses [NavigationBarView.setOnItemSelectedListener]. Only one
 * channel can be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      anyNavigationBarView.itemSelections(scope)
 *          .consumeEach { /* handle selected item */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun NavigationBarView.itemSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<MenuItem> = corbindReceiveChannel(scope, capacity) {
    setInitialValue(this@itemSelections) {
        sendInitialValue(it)
        true
    }
    setOnItemSelectedListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnItemSelectedListener(null) }
}

/**
 * Create a flow which emits the selected item in [NavigationBarView].
 *
 * *Warning:* The created flow uses [NavigationBarView.setOnItemSelectedListener]. Only one flow can
 * be used at a time.
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * anyNavigationBarView.itemSelections()
 *      .onEach { /* handle selected item */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * anyNavigationBarView.itemSelections()
 *      .drop(1)
 *      .onEach { /* handle selected item */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun NavigationBarView.itemSelections(): Flow<MenuItem> = corbindCallbackFlow {
    setInitialValue(this@itemSelections, corbindEventEmitter())
    setOnItemSelectedListener(listener(this, corbindEventEmitter()))
    awaitClose { setOnItemSelectedListener(null) }
}

private fun setInitialValue(
    navigationBarView: NavigationBarView,
    emitter: (MenuItem) -> Boolean,
) {
    for (item in navigationBarView.menu) {
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
) = NavigationBarView.OnItemSelectedListener {
    if (scope.isActive) {
        return@OnItemSelectedListener emitter(it)
    }
    return@OnItemSelectedListener false
}

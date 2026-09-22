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

import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import com.google.android.material.tabs.TabLayout
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
 * Perform an action on the selected tab in [TabLayout].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun TabLayout.selections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TabLayout.Tab) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<TabLayout.Tab>(Dispatchers.Main.immediate, capacity) {
        for (tab in channel) action(tab)
    }

    setInitialValue(this, events.corbindEventEmitter(scope))
    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnTabSelectedListener(listener)
    events.invokeOnCloseOnMain { removeOnTabSelectedListener(listener) }
}

/**
 * Perform an action on the selected tab in [TabLayout], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun TabLayout.selections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TabLayout.Tab) -> Unit,
) = coroutineScope {
    selections(this, capacity, action)
}

/**
 * Create a channel which emits the selected tab in [TabLayout].
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      tabLayout.selections(scope)
 *          .consumeEach { /* handle selected tab */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun TabLayout.selections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<TabLayout.Tab> = corbindReceiveChannel(scope, capacity) {
    setInitialValue(this@selections) {
        sendInitialValue(it)
        true
    }
    val listener = listener(scope, corbindEventEmitter())
    addOnTabSelectedListener(listener)
    awaitClose { removeOnTabSelectedListener(listener) }
}

/**
 * Create a flow which emits the selected tab in [TabLayout].
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * tabLayout.selections()
 *      .onEach { /* handle selected tab */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * tabLayout.selections()
 *      .drop(1)
 *      .onEach { /* handle selected tab */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun TabLayout.selections(): Flow<TabLayout.Tab> = corbindCallbackFlow {
    setInitialValue(this@selections, corbindEventEmitter())
    val listener = listener(this, corbindEventEmitter())
    addOnTabSelectedListener(listener)
    awaitClose { removeOnTabSelectedListener(listener) }
}

private fun setInitialValue(
    tabLayout: TabLayout,
    emitter: (TabLayout.Tab) -> Boolean,
) {
    val index = tabLayout.selectedTabPosition
    if (index != -1) emitter(tabLayout.getTabAt(index)!!)
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (TabLayout.Tab) -> Boolean,
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) {
        if (scope.isActive) emitter(tab)
    }

    override fun onTabReselected(tab: TabLayout.Tab) = Unit
    override fun onTabUnselected(tab: TabLayout.Tab) = Unit
}

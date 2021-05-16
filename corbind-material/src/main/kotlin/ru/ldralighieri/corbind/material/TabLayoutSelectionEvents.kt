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
import com.google.android.material.tabs.TabLayout
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

sealed class TabLayoutSelectionEvent {
    abstract val view: TabLayout
    abstract val tab: TabLayout.Tab
}

data class TabLayoutSelectionSelectedEvent(
    override val view: TabLayout,
    override val tab: TabLayout.Tab
) : TabLayoutSelectionEvent()

data class TabLayoutSelectionReselectedEvent(
    override val view: TabLayout,
    override val tab: TabLayout.Tab
) : TabLayoutSelectionEvent()

data class TabLayoutSelectionUnselectedEvent(
    override val view: TabLayout,
    override val tab: TabLayout.Tab
) : TabLayoutSelectionEvent()

/**
 * Perform an action on selection, reselection, and unselection [events][TabLayoutSelectionEvent]
 * for the tabs in [TabLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TabLayout.selectionEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TabLayoutSelectionEvent) -> Unit
) {
    val events = scope.actor<TabLayoutSelectionEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    setInitialValue(this, events::trySend)
    val listener = listener(scope, this, events::trySend)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}

/**
 * Perform an action on selection, reselection, and unselection [events][TabLayoutSelectionEvent]
 * for the tabs in [TabLayout], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TabLayout.selectionEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TabLayoutSelectionEvent) -> Unit
) = coroutineScope {
    selectionEvents(this, capacity, action)
}

/**
 * Create a channel which emits selection, reselection, and unselection
 * [events][TabLayoutSelectionEvent] for the tabs in [TabLayout].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      tabLayout.selectionEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is TabLayoutSelectionSelectedEvent -> { /* handle select event */ }
 *                  is TabLayoutSelectionReselectedEvent -> { /* handle reselect event */ }
 *                  is TabLayoutSelectionUnselectedEvent -> { /* handle unselect event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      tabLayout.selectionEvents(scope)
 *          .filterIsInstance<TabLayoutSelectionSelectedEvent>()
 *          .consumeEach { /* handle event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TabLayout.selectionEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TabLayoutSelectionEvent> = corbindReceiveChannel(capacity) {
    setInitialValue(this@selectionEvents, ::trySend)
    val listener = listener(scope, this@selectionEvents, ::trySend)
    addOnTabSelectedListener(listener)
    invokeOnClose { removeOnTabSelectedListener(listener) }
}

/**
 * Create a flow which emits selection, reselection, and unselection
 * [events][TabLayoutSelectionEvent] for the tabs in [TabLayout].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * tabLayout.selectionEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is TabLayoutSelectionSelectedEvent -> { /* handle select event */ }
 *              is TabLayoutSelectionReselectedEvent -> { /* handle reselect event */ }
 *              is TabLayoutSelectionUnselectedEvent -> { /* handle unselect event */ }
 *          }
 *      }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // handle one event
 * tabLayout.selectionEvents()
 *      .filterIsInstance<TabLayoutSelectionSelectedEvent>()
 *      .onEach { /* handle select event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * tabLayout.selectionEvents()
 *      .drop(1)
 *      .onEach { /* handle event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun TabLayout.selectionEvents(): Flow<TabLayoutSelectionEvent> = channelFlow {
    setInitialValue(this@selectionEvents, ::trySend)
    val listener = listener(this, this@selectionEvents, ::trySend)
    addOnTabSelectedListener(listener)
    awaitClose { removeOnTabSelectedListener(listener) }
}

private fun setInitialValue(
    tabLayout: TabLayout,
    emitter: (TabLayoutSelectionEvent) -> Unit
) {
    val index = tabLayout.selectedTabPosition
    if (index != -1) {
        emitter(TabLayoutSelectionSelectedEvent(tabLayout, tabLayout.getTabAt(index)!!))
    }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    tabLayout: TabLayout,
    emitter: (TabLayoutSelectionEvent) -> Unit
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) {
        onEvent(TabLayoutSelectionSelectedEvent(tabLayout, tab))
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        onEvent(TabLayoutSelectionUnselectedEvent(tabLayout, tab))
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        onEvent(TabLayoutSelectionReselectedEvent(tabLayout, tab))
    }

    private fun onEvent(event: TabLayoutSelectionEvent) {
        if (scope.isActive) { emitter(event) }
    }
}

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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on the selected tab in [TabLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TabLayout.selections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TabLayout.Tab) -> Unit
) {
    val events = scope.actor<TabLayout.Tab>(Dispatchers.Main, capacity) {
        for (tab in channel) action(tab)
    }

    setInitialValue(this, events::offer)
    val listener = listener(scope, events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}

/**
 * Perform an action on the selected tab in [TabLayout], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TabLayout.selections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TabLayout.Tab) -> Unit
) = coroutineScope {
    selections(this, capacity, action)
}

/**
 * Create a channel which emits the selected tab in [TabLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TabLayout.selections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TabLayout.Tab> = corbindReceiveChannel(capacity) {
    setInitialValue(this@selections, ::offerElement)
    val listener = listener(scope, ::offerElement)
    addOnTabSelectedListener(listener)
    invokeOnClose { removeOnTabSelectedListener(listener) }
}

/**
 * Create a flow which emits the selected tab in [TabLayout].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun TabLayout.selections(): Flow<TabLayout.Tab> = channelFlow {
    setInitialValue(this@selections, ::offer)
    val listener = listener(this, ::offer)
    addOnTabSelectedListener(listener)
    awaitClose { removeOnTabSelectedListener(listener) }
}

private fun setInitialValue(
    tabLayout: TabLayout,
    emitter: (TabLayout.Tab) -> Boolean
) {
    val index = tabLayout.selectedTabPosition
    if (index != -1) { emitter(tabLayout.getTabAt(index)!!) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (TabLayout.Tab) -> Boolean
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) {
        if (scope.isActive) { emitter(tab) }
    }

    override fun onTabReselected(tab: TabLayout.Tab) { }
    override fun onTabUnselected(tab: TabLayout.Tab) { }
}

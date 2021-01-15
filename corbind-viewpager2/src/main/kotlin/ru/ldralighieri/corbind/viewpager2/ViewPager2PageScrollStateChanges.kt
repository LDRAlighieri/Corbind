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

package ru.ldralighieri.corbind.viewpager2

import androidx.annotation.CheckResult
import androidx.viewpager2.widget.ViewPager2
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
 * Perform an action on scroll state change events on [ViewPager2].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ViewPager2.pageScrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val callback = callback(scope, events::offer)
    registerOnPageChangeCallback(callback)
    events.invokeOnClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Perform an action on scroll state change events on [ViewPager2], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ViewPager2.pageScrollStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    pageScrollStateChanges(this, capacity, action)
}

/**
 * Create a channel of scroll state change events on [ViewPager2].
 *
 * Example:
 *
 * ```
 * launch {
 *      viewPager.pageScrollStateChanges(scope)
 *          .consumeEach { /* handle scroll state change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ViewPager2.pageScrollStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, ::offerCatching)
    registerOnPageChangeCallback(callback)
    invokeOnClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Create a flow of scroll state change events on [ViewPager2].
 *
 * Example:
 *
 * ```
 * viewPager2.pageScrollStateChanges()
 *      .onEach { /* handle scroll state change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun ViewPager2.pageScrollStateChanges(): Flow<Int> = channelFlow {
    val callback = callback(this, ::offerCatching)
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : ViewPager2.OnPageChangeCallback() {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit
    override fun onPageSelected(position: Int) = Unit

    override fun onPageScrollStateChanged(state: Int) {
        if (scope.isActive) { emitter(state) }
    }
}

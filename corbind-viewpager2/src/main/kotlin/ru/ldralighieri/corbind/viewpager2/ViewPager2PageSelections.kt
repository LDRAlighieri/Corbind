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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on page selected events on [ViewPager2].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ViewPager2.pageSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (position in channel) action(position)
    }

    events.offer(currentItem)
    val callback = callback(scope, events::offer)
    registerOnPageChangeCallback(callback)
    events.invokeOnClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Perform an action on page selected events on [ViewPager2], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ViewPager2.pageSelections(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    pageSelections(this, capacity, action)
}

/**
 * Create a channel of page selected events on [ViewPager2].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      viewPager.pageSelections(scope)
 *          .consumeEach { /* handle selected page */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ViewPager2.pageSelections(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    offerCatching(currentItem)
    val callback = callback(scope, ::offerCatching)
    registerOnPageChangeCallback(callback)
    invokeOnClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Create a flow of page selected events on [ViewPager2].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * viewPager2.pageSelections()
 *      .onEach { /* handle selected page */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * viewPager2.pageSelections()
 *      .dropInitialValue()
 *      .onEach { /* handle selected page */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun ViewPager2.pageSelections(): InitialValueFlow<Int> = channelFlow {
    val callback = callback(this, ::offerCatching)
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}.asInitialValueFlow(currentItem)

@CheckResult
private fun callback(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : ViewPager2.OnPageChangeCallback() {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) = Unit

    override fun onPageSelected(position: Int) {
        if (scope.isActive) { emitter(position) }
    }

    override fun onPageScrollStateChanged(state: Int) = Unit
}

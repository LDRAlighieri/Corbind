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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.safeOffer

data class ViewPager2PageScrollEvent(
    val viewPager: ViewPager2,
    val position: Int,
    val positionOffset: Float,
    val positionOffsetPixels: Int
)

/**
 * Perform an action on [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ViewPager2.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPager2PageScrollEvent) -> Unit
) {
    val events = scope.actor<ViewPager2PageScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val callback = callback(scope, this, events::offer)
    registerOnPageChangeCallback(callback)
    events.invokeOnClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Perform an action on [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ViewPager2.pageScrollEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPager2PageScrollEvent) -> Unit
) = coroutineScope {
    pageScrollEvents(this, capacity, action)
}

/**
 * Create a channel of [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2].
 *
 * Example:
 *
 * ```
 * launch {
 *      viewPager2.pageScrollEvents(scope)
 *          .consumeEach { /* handle page scroll event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ViewPager2.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewPager2PageScrollEvent> = corbindReceiveChannel(capacity) {
    val callback = callback(scope, this@pageScrollEvents, ::safeOffer)
    registerOnPageChangeCallback(callback)
    invokeOnClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Create a flow of [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2].
 *
 * Example:
 *
 * ```
 * viewPager2.pageScrollEvents()
 *      .onEach { /* handle page scroll event */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun ViewPager2.pageScrollEvents(): Flow<ViewPager2PageScrollEvent> = channelFlow {
    val callback = callback(this, this@pageScrollEvents, ::offer)
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    viewPager: ViewPager2,
    emitter: (ViewPager2PageScrollEvent) -> Boolean
) = object : ViewPager2.OnPageChangeCallback() {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (scope.isActive) {
            val event = ViewPager2PageScrollEvent(viewPager, position, positionOffset,
                    positionOffsetPixels)
            emitter(event)
        }
    }

    override fun onPageSelected(position: Int) { }
    override fun onPageScrollStateChanged(state: Int) { }
}

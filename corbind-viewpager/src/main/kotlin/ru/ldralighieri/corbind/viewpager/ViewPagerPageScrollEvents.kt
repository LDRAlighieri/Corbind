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

package ru.ldralighieri.corbind.viewpager

import androidx.annotation.CheckResult
import androidx.viewpager.widget.ViewPager
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

data class ViewPagerPageScrollEvent(
    val viewPager: ViewPager,
    val position: Int,
    val positionOffset: Float,
    val positionOffsetPixels: Int
)

/**
 * Perform an action on [page scroll events][ViewPagerPageScrollEvent] on [ViewPager].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun ViewPager.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPagerPageScrollEvent) -> Unit
) {
    val events = scope.actor<ViewPagerPageScrollEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, this, events::trySend)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}

/**
 * Perform an action on [page scroll events][ViewPagerPageScrollEvent] on [ViewPager], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun ViewPager.pageScrollEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPagerPageScrollEvent) -> Unit
) = coroutineScope {
    pageScrollEvents(this, capacity, action)
}

/**
 * Create a channel of [page scroll events][ViewPagerPageScrollEvent] on [ViewPager].
 *
 * Example:
 *
 * ```
 * launch {
 *      viewPager.pageScrollEvents(scope)
 *          .consumeEach { /* handle page scroll event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun ViewPager.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewPagerPageScrollEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, this@pageScrollEvents, ::trySend)
    addOnPageChangeListener(listener)
    invokeOnClose { removeOnPageChangeListener(listener) }
}

/**
 * Create a flow of [page scroll events][ViewPagerPageScrollEvent] on [ViewPager].
 *
 * Example:
 *
 * ```
 * viewPager.pageScrollEvents()
 *      .onEach { /* handle page scroll event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun ViewPager.pageScrollEvents(): Flow<ViewPagerPageScrollEvent> = channelFlow {
    val listener = listener(this, this@pageScrollEvents, ::trySend)
    addOnPageChangeListener(listener)
    awaitClose { removeOnPageChangeListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    viewPager: ViewPager,
    emitter: (ViewPagerPageScrollEvent) -> Unit
) = object : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (scope.isActive) {
            val event = ViewPagerPageScrollEvent(
                viewPager = viewPager,
                position = position,
                positionOffset = positionOffset,
                positionOffsetPixels = positionOffsetPixels
            )
            emitter(event)
        }
    }

    override fun onPageSelected(position: Int) = Unit
    override fun onPageScrollStateChanged(state: Int) = Unit
}

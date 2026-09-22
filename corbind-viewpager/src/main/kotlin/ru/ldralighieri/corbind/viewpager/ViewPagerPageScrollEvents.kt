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
import androidx.annotation.MainThread
import androidx.viewpager.widget.ViewPager
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

data class ViewPagerPageScrollEvent(
    val viewPager: ViewPager,
    val position: Int,
    val positionOffset: Float,
    val positionOffsetPixels: Int,
)

/**
 * Perform an action on [page scroll events][ViewPagerPageScrollEvent] on [ViewPager].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun ViewPager.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPagerPageScrollEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<ViewPagerPageScrollEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, this, events.corbindEventEmitter(scope))
    addOnPageChangeListener(listener)
    events.invokeOnCloseOnMain { removeOnPageChangeListener(listener) }
}

/**
 * Perform an action on [page scroll events][ViewPagerPageScrollEvent] on [ViewPager], in a new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun ViewPager.pageScrollEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPagerPageScrollEvent) -> Unit,
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun ViewPager.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<ViewPagerPageScrollEvent> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, this@pageScrollEvents, corbindEventEmitter())
    addOnPageChangeListener(listener)
    awaitClose { removeOnPageChangeListener(listener) }
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
fun ViewPager.pageScrollEvents(): Flow<ViewPagerPageScrollEvent> = corbindCallbackFlow {
    val listener = listener(this, this@pageScrollEvents, corbindEventEmitter())
    addOnPageChangeListener(listener)
    awaitClose { removeOnPageChangeListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    viewPager: ViewPager,
    emitter: (ViewPagerPageScrollEvent) -> Boolean,
) = object : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (scope.isActive) {
            val event = ViewPagerPageScrollEvent(
                viewPager = viewPager,
                position = position,
                positionOffset = positionOffset,
                positionOffsetPixels = positionOffsetPixels,
            )
            emitter(event)
        }
    }

    override fun onPageSelected(position: Int) = Unit
    override fun onPageScrollStateChanged(state: Int) = Unit
}

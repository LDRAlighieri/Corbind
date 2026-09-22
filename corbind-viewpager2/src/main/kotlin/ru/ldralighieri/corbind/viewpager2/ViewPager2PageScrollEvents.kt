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
import androidx.annotation.MainThread
import androidx.viewpager2.widget.ViewPager2
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

data class ViewPager2PageScrollEvent(
    val viewPager: ViewPager2,
    val position: Int,
    val positionOffset: Float,
    val positionOffsetPixels: Int,
)

/**
 * Perform an action on [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun ViewPager2.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPager2PageScrollEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<ViewPager2PageScrollEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val callback = callback(scope, this, events.corbindEventEmitter(scope))
    registerOnPageChangeCallback(callback)
    events.invokeOnCloseOnMain { unregisterOnPageChangeCallback(callback) }
}

/**
 * Perform an action on [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2], in a new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun ViewPager2.pageScrollEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewPager2PageScrollEvent) -> Unit,
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun ViewPager2.pageScrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<ViewPager2PageScrollEvent> = corbindReceiveChannel(scope, capacity) {
    val callback = callback(scope, this@pageScrollEvents, corbindEventEmitter())
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}

/**
 * Create a flow of [page scroll events][ViewPager2PageScrollEvent] on [ViewPager2].
 *
 * Example:
 *
 * ```
 * viewPager2.pageScrollEvents()
 *      .onEach { /* handle page scroll event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun ViewPager2.pageScrollEvents(): Flow<ViewPager2PageScrollEvent> = corbindCallbackFlow {
    val callback = callback(this, this@pageScrollEvents, corbindEventEmitter())
    registerOnPageChangeCallback(callback)
    awaitClose { unregisterOnPageChangeCallback(callback) }
}

@CheckResult
private fun callback(
    scope: CoroutineScope,
    viewPager: ViewPager2,
    emitter: (ViewPager2PageScrollEvent) -> Boolean,
) = object : ViewPager2.OnPageChangeCallback() {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (scope.isActive) {
            val event = ViewPager2PageScrollEvent(
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

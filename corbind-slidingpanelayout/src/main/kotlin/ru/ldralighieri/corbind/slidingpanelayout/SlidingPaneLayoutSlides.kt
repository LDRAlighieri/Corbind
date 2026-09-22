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

package ru.ldralighieri.corbind.slidingpanelayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.slidingpanelayout.widget.SlidingPaneLayout
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

/**
 * Perform an action on the slide offset of the pane of [SlidingPaneLayout].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun SlidingPaneLayout.panelSlides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (slide in channel) action(slide)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addPanelSlideListener(listener)
    events.invokeOnCloseOnMain { removePanelSlideListener(listener) }
}

/**
 * Perform an action on the slide offset of the pane of [SlidingPaneLayout], in a new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun SlidingPaneLayout.panelSlides(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) = coroutineScope {
    panelSlides(this, capacity, action)
}

/**
 * Create a channel of the slide offset of the pane of [SlidingPaneLayout].
 *
 * Example:
 *
 * ```
 * launch {
 *      slidingPaneLayout.panelSlides(scope)
 *          .consumeEach { /* handle slide offset */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun SlidingPaneLayout.panelSlides(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Float> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addPanelSlideListener(listener)
    awaitClose { removePanelSlideListener(listener) }
}

/**
 * Create a flow of the slide offset of the pane of [SlidingPaneLayout].
 *
 * Example:
 *
 * ```
 * slidingPaneLayout.panelSlides()
 *      .onEach { /* handle slide offset */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SlidingPaneLayout.panelSlides(): Flow<Float> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addPanelSlideListener(listener)
    awaitClose { removePanelSlideListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Float) -> Boolean,
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        if (scope.isActive) emitter(slideOffset)
    }

    override fun onPanelOpened(panel: View) = Unit
    override fun onPanelClosed(panel: View) = Unit
}

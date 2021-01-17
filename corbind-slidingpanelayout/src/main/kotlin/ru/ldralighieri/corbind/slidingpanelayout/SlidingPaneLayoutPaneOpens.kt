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
import androidx.slidingpanelayout.widget.SlidingPaneLayout
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
 * Perform an action on the open state of the pane of [SlidingPaneLayout].
 *
 * *Warning:* The created actor uses [SlidingPaneLayout.setPanelSlideListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SlidingPaneLayout.panelOpens(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.offer(isOpen)
    setPanelSlideListener(listener(scope, events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}

/**
 * Perform an action on the open state of the pane of [SlidingPaneLayout], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SlidingPaneLayout.setPanelSlideListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SlidingPaneLayout.panelOpens(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    panelOpens(this, capacity, action)
}

/**
 * Create a channel of the open state of the pane of [SlidingPaneLayout].
 *
 * *Warning:* The created channel uses [SlidingPaneLayout.setPanelSlideListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      slidingPaneLayout.panelOpens(scope)
 *          .consumeEach { /* handle open state */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SlidingPaneLayout.panelOpens(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    offerCatching(isOpen)
    setPanelSlideListener(listener(scope, ::offerCatching))
    invokeOnClose { setPanelSlideListener(null) }
}

/**
 * Create a flow of the open state of the pane of [SlidingPaneLayout].
 *
 * *Warning:* The created flow uses [SlidingPaneLayout.setPanelSlideListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * slidingPaneLayout.panelOpens()
 *      .onEach { /* handle open state */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * slidingPaneLayout.panelOpens()
 *      .dropInitialValue()
 *      .onEach { /* handle open state */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun SlidingPaneLayout.panelOpens(): InitialValueFlow<Boolean> = channelFlow {
    setPanelSlideListener(listener(this, ::offerCatching))
    awaitClose { setPanelSlideListener(null) }
}.asInitialValueFlow(isOpen)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) = Unit
    override fun onPanelOpened(panel: View) { onEvent(true) }
    override fun onPanelClosed(panel: View) { onEvent(false) }

    private fun onEvent(event: Boolean) {
        if (scope.isActive) { emitter(event) }
    }
}

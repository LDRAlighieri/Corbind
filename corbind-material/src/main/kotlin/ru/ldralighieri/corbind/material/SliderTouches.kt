/*
 * Copyright 2020 Vladimir Raupov
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
import com.google.android.material.slider.Slider
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

/**
 * Perform an action on touch tracking events for [Slider].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Slider.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::trySend)
    addOnSliderTouchListener(listener)
    events.invokeOnClose { removeOnSliderTouchListener(listener) }
}

/**
 * Perform an action on touch tracking events for [Slider], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Slider.touches(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    touches(this, capacity, action)
}

/**
 * Create a channel of touch tracking events for [Slider].
 *
 * Example:
 *
 * ```
 * launch {
 *      slider.touches(scope)
 *          .consumeEach { /* handle touch tracking event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Slider.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addOnSliderTouchListener(listener)
    invokeOnClose { removeOnSliderTouchListener(listener) }
}

/**
 * Create a flow of touch tracking events for [Slider].
 *
 * Examples:
 *
 * ```
 * slider.touches()
 *      .onEach { /* handle touch tracking event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun Slider.touches(): Flow<Boolean> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnSliderTouchListener(listener)
    awaitClose { removeOnSliderTouchListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Unit
) = object : Slider.OnSliderTouchListener {

    override fun onStartTrackingTouch(slider: Slider) { onEvent(true) }
    override fun onStopTrackingTouch(slider: Slider) { onEvent(false) }

    private fun onEvent(event: Boolean) {
        if (scope.isActive) { emitter(event) }
    }
}

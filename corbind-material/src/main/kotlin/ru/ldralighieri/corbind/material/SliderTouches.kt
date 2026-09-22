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
import androidx.annotation.MainThread
import com.google.android.material.slider.Slider
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
 * Perform an action on touch tracking events for [Slider].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun Slider.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnSliderTouchListener(listener)
    events.invokeOnCloseOnMain { removeOnSliderTouchListener(listener) }
}

/**
 * Perform an action on touch tracking events for [Slider], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun Slider.touches(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit,
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun Slider.touches(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Boolean> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addOnSliderTouchListener(listener)
    awaitClose { removeOnSliderTouchListener(listener) }
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
fun Slider.touches(): Flow<Boolean> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addOnSliderTouchListener(listener)
    awaitClose { removeOnSliderTouchListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean,
) = object : Slider.OnSliderTouchListener {

    override fun onStartTrackingTouch(slider: Slider) = onEvent(true)
    override fun onStopTrackingTouch(slider: Slider) = onEvent(false)

    private fun onEvent(event: Boolean) {
        if (scope.isActive) emitter(event)
    }
}

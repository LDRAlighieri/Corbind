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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

data class SliderChangeEvent(
    val view: Slider,
    val value: Float,
    val previousValue: Float,
    val fromUser: Boolean
)

/**
 * Perform an action on [value change events][SliderChangeEvent] changes on [Slider].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Slider.valueChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SliderChangeEvent) -> Unit
) {
    val events = scope.actor<SliderChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val event = initialValue(this@valueChangeEvents).also { events.trySend(it) }
    val listener = listener(scope, events::trySend).apply { previousValue = event.previousValue }
    addOnChangeListener(listener)
    events.invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Perform an action on [value change events][SliderChangeEvent] changes on [Slider], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Slider.valueChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SliderChangeEvent) -> Unit
) = coroutineScope {
    valueChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [value change events][SliderChangeEvent] on [Slider].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      slider.valueChangeEvents(scope)
 *          .consumeEach { /* handle value change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Slider.valueChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SliderChangeEvent> = corbindReceiveChannel(capacity) {
    val event = initialValue(this@valueChangeEvents).also(::trySend)
    val listener = listener(scope, ::trySend).apply { previousValue = event.previousValue }
    addOnChangeListener(listener)
    invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Create a flow of the [value change events][SliderChangeEvent] on [Slider].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * slider.valueChangeEvents()
 *      .onEach { /* handle value change event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * slider.valueChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle value change event */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun Slider.valueChangeEvents(): InitialValueFlow<SliderChangeEvent> = channelFlow {
    val listener = listener(this, ::trySend).apply { previousValue = value }
    addOnChangeListener(listener)
    awaitClose { removeOnChangeListener(listener) }
}.asInitialValueFlow(initialValue(slider = this))

@CheckResult
private fun initialValue(slider: Slider): SliderChangeEvent =
    SliderChangeEvent(slider, slider.value, slider.value, false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (SliderChangeEvent) -> Unit
) = object : Slider.OnChangeListener {

    var previousValue: Float = Float.NaN
    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        if (scope.isActive) { emitter(SliderChangeEvent(slider, value, previousValue, fromUser)) }
    }
}

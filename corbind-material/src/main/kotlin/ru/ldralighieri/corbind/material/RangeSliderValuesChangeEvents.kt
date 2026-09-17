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
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.sendInitialValue

enum class RangeSliderSide { INIT, LEFT, RIGHT }

data class RangeSliderChangeEvent(
    val view: RangeSlider,
    val changedSide: RangeSliderSide,
    val newValues: List<Float>,
    val previousValues: List<Float>,
    val fromUser: Boolean,
)

/**
 * Perform an action on [values change events][RangeSliderChangeEvent] on [RangeSlider].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
fun RangeSlider.valuesChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RangeSliderChangeEvent) -> Unit,
) {
    val events = scope.actor<RangeSliderChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val event = initialValue(this@valuesChangeEvents).also { events.corbindEventEmitter(scope)(it) }
    val listener = listener(scope, events.corbindEventEmitter(scope)).apply { previousValues = event.previousValues }
    addOnChangeListener(listener)
    events.invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Perform an action on [values change events][RangeSliderChangeEvent] on [RangeSlider], inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
suspend fun RangeSlider.valuesChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (RangeSliderChangeEvent) -> Unit,
) = coroutineScope {
    valuesChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [values change events][RangeSliderChangeEvent] on [RangeSlider].
 *
 * *Note:* A values will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      rangeSlider.valuesChangeEvents(scope)
 *          .consumeEach { /* handle values change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun RangeSlider.valuesChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<RangeSliderChangeEvent> = corbindReceiveChannel(scope, capacity) {
    val event = initialValue(this@valuesChangeEvents).also(::sendInitialValue)
    val listener = listener(scope, corbindEventEmitter()).apply { previousValues = event.previousValues }
    addOnChangeListener(listener)
    awaitClose { removeOnChangeListener(listener) }
}

/**
 * Create a flow of [values change events][RangeSliderChangeEvent] on [RangeSlider].
 *
 * *Note:* A values will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial values
 * rangeSlider.valuesChangeEvents()
 *      .onEach { /* handle values change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial values
 * rangeSlider.valuesChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle values change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RangeSlider.valuesChangeEvents(): InitialValueFlow<RangeSliderChangeEvent> = callbackFlow {
    val emitter = initialValueFlowEmitter()
    val listener = listener(this, emitter).apply {
        previousValues = this@valuesChangeEvents.values
    }
    addOnChangeListener(listener)
    val event = initialValue(this@valuesChangeEvents)
    listener.previousValues = event.previousValues
    emitter.sendInitialValue(event)
    awaitClose { removeOnChangeListener(listener) }
}.asInitialValueFlow()

@CheckResult
private fun initialValue(slider: RangeSlider): RangeSliderChangeEvent {
    val values = slider.values
    return RangeSliderChangeEvent(slider, RangeSliderSide.INIT, values, values, false)
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (RangeSliderChangeEvent) -> Boolean,
) = object : RangeSlider.OnChangeListener {

    var previousValues: List<Float> = mutableListOf()
    override fun onValueChange(slider: RangeSlider, value: Float, fromUser: Boolean) {
        val values = slider.values
        if (scope.isActive) {
            val changedSide =
                if (previousValues[0] != values[0]) {
                    RangeSliderSide.LEFT
                } else {
                    RangeSliderSide.RIGHT
                }

            val event =
                RangeSliderChangeEvent(slider, changedSide, values, previousValues, fromUser)

            previousValues = values
            emitter(event)
        }
    }
}

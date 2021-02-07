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

package ru.ldralighieri.corbind.widget

import android.widget.NumberPicker
import androidx.annotation.CheckResult
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

data class NumberPickerValueChangeEvent(
    val picker: NumberPicker,
    val oldVal: Int,
    val newVal: Int
)

/**
 * Perform an action on [value change events][NumberPickerValueChangeEvent] on [NumberPicker].
 *
 * *Warning:* The created actor uses [NumberPicker.setOnValueChangedListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun NumberPicker.valueChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (NumberPickerValueChangeEvent) -> Unit
) {
    val events = scope.actor<NumberPickerValueChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.offer(NumberPickerValueChangeEvent(this, value, value))
    setOnValueChangedListener(listener(scope, events::offer))
    events.invokeOnClose { setOnValueChangedListener(null) }
}

/**
 * Perform an action on [value change events][NumberPickerValueChangeEvent] on [NumberPicker],
 * inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [NumberPicker.setOnValueChangedListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun NumberPicker.valueChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (NumberPickerValueChangeEvent) -> Unit
) = coroutineScope {
    valueChangeEvents(this, capacity, action)
}

/**
 * Create a channel which emits on [value change events][NumberPickerValueChangeEvent] on
 * [NumberPicker].
 *
 * *Warning:* The created channel uses [NumberPicker.setOnValueChangedListener]. Only one channel
 * can be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      numberPicker.valueChangeEvents(scope)
 *          .consumeEach { /* handle value change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NumberPicker.valueChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<NumberPickerValueChangeEvent> = corbindReceiveChannel(capacity) {
    offerCatching(NumberPickerValueChangeEvent(this@valueChangeEvents, value, value))
    setOnValueChangedListener(listener(scope, ::offerCatching))
    invokeOnClose { setOnValueChangedListener(null) }
}

/**
 * Create a flow which emits on [value change events][NumberPickerValueChangeEvent] on
 * [NumberPicker].
 *
 * *Warning:* The created flow uses [NumberPicker.setOnValueChangedListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * numberPicker.valueChangeEvents()
 *      .onEach { /* handle value change event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * numberPicker.valueChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle value change event */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun NumberPicker.valueChangeEvents(): InitialValueFlow<NumberPickerValueChangeEvent> =
    channelFlow<NumberPickerValueChangeEvent> {
        setOnValueChangedListener(listener(this, ::offerCatching))
        awaitClose { setOnValueChangedListener(null) }
    }.asInitialValueFlow(NumberPickerValueChangeEvent(picker = this, value, value))

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (NumberPickerValueChangeEvent) -> Boolean
) = NumberPicker.OnValueChangeListener { picker, oldVal, newVal ->
    if (scope.isActive) { emitter(NumberPickerValueChangeEvent(picker, oldVal, newVal)); }
}

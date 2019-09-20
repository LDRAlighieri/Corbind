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
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

data class NumberPickerValueChangeEvent(
    val picker: NumberPicker,
    val oldVal: Int,
    val newVal: Int
)

/**
 * Perform an action on [NumberPicker] value change events.
 *
 * *Note:* An action will be performed immediately.
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
    val events = scope.actor<NumberPickerValueChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(NumberPickerValueChangeEvent(this, value, value))
    setOnValueChangedListener(listener(scope, events::offer))
    events.invokeOnClose { setOnValueChangedListener(null) }
}

/**
 * Perform an action on [NumberPicker] value change events, inside new [CoroutineScope].
 *
 * *Note:* An action will be performed immediately.
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
 * Create a channel which emits on [NumberPicker] value change events.
 *
 * *Note:* A value will be emitted immediately on consume.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun NumberPicker.valueChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<NumberPickerValueChangeEvent> = corbindReceiveChannel(capacity) {
    offer(NumberPickerValueChangeEvent(this@valueChangeEvents, value, value))
    setOnValueChangedListener(listener(scope, ::offerElement))
    invokeOnClose { setOnValueChangedListener(null) }
}

/**
 * Create a flow which emits on [NumberPicker] value change events.
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun NumberPicker.valueChangeEvents(): Flow<NumberPickerValueChangeEvent> = channelFlow {
    offer(NumberPickerValueChangeEvent(this@valueChangeEvents, value, value))
    setOnValueChangedListener(listener(this, ::offer))
    awaitClose { setOnValueChangedListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (NumberPickerValueChangeEvent) -> Boolean
) = NumberPicker.OnValueChangeListener { picker, oldVal, newVal ->
    if (scope.isActive) { emitter(NumberPickerValueChangeEvent(picker, oldVal, newVal)); }
}

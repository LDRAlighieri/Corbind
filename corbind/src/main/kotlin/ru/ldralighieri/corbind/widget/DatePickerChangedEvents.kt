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

import android.os.Build
import android.widget.DatePicker
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
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

data class DateChangedEvent(
    val view: DatePicker,
    val year: Int,
    val monthOfYear: Int,
    val dayOfMonth: Int
)

/**
 * Perform an action on [date changed events][DateChangedEvent] on [DatePicker].
 *
 * *Warning:* The created actor uses [DatePicker.setOnDateChangedListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.O)
fun DatePicker.dateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (DateChangedEvent) -> Unit
) {
    val events = scope.actor<DateChangedEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.trySend(DateChangedEvent(this, year, month, dayOfMonth))
    setOnDateChangedListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnDateChangedListener(null) }
}

/**
 * Perform an action on [date changed events][DateChangedEvent] on [DatePicker], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [DatePicker.setOnDateChangedListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.O)
suspend fun DatePicker.dateChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (DateChangedEvent) -> Unit
) = coroutineScope {
    dateChangeEvents(this, capacity, action)
}

/**
 * Create a channel which emits on [date changed events][DateChangedEvent] on [DatePicker].
 *
 * *Warning:* The created channel uses [DatePicker.setOnDateChangedListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      datePicker.dateChangeEvents(scope)
 *          .consumeEach { /* handle date changed event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.O)
@CheckResult
fun DatePicker.dateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<DateChangedEvent> = corbindReceiveChannel(capacity) {
    trySend(DateChangedEvent(this@dateChangeEvents, year, month, dayOfMonth))
    setOnDateChangedListener(listener(scope, ::trySend))
    invokeOnClose { setOnDateChangedListener(null) }
}

/**
 * Create a flow which emits on [date changed events][DateChangedEvent] on [DatePicker].
 *
 * *Warning:* The created flow uses [DatePicker.setOnDateChangedListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * datePicker.dateChangeEvents()
 *      .onEach { /* handle date changed event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * datePicker.dateChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle date changed event */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@RequiresApi(Build.VERSION_CODES.O)
@CheckResult
fun DatePicker.dateChangeEvents(): InitialValueFlow<DateChangedEvent> = channelFlow {
    setOnDateChangedListener(listener(this, ::trySend))
    awaitClose { setOnDateChangedListener(null) }
}.asInitialValueFlow(DateChangedEvent(view = this, year, month, dayOfMonth))

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (DateChangedEvent) -> Unit
) = DatePicker.OnDateChangedListener { view, year, monthOfYear, dayOfMonth ->
    if (scope.isActive) { emitter(DateChangedEvent(view, year, monthOfYear, dayOfMonth)) }
}

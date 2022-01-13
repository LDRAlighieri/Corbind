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

import android.widget.CalendarView
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
import java.util.Calendar

data class CalendarViewDateChangeEvent(
    val view: CalendarView,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int
)

/**
 * Perform an action on [date change events][CalendarViewDateChangeEvent] on [CalendarView].
 *
 * *Warning:* The created actor uses [CalendarView.setOnDateChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun CalendarView.dateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CalendarViewDateChangeEvent) -> Unit
) {
    val events = scope.actor<CalendarViewDateChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.trySend(initialValue(this))
    setOnDateChangeListener(listener(scope, events::trySend))
    events.invokeOnClose { setOnDateChangeListener(null) }
}

/**
 * Perform an action on [date change events][CalendarViewDateChangeEvent] on [CalendarView], inside
 * new [CoroutineScope].
 *
 * *Warning:* The created actor uses [CalendarView.setOnDateChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun CalendarView.dateChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (CalendarViewDateChangeEvent) -> Unit
) = coroutineScope {
    dateChangeEvents(this, capacity, action)
}

/**
 * Create a channel which emits on [date change events][CalendarViewDateChangeEvent] on
 * [CalendarView].
 *
 * *Warning:* The created channel uses [CalendarView.setOnDateChangeListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      calendarView.dateChangeEvents(scope)
 *          .consumeEach { /* handle date change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun CalendarView.dateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<CalendarViewDateChangeEvent> = corbindReceiveChannel(capacity) {
    trySend(initialValue(this@dateChangeEvents))
    setOnDateChangeListener(listener(scope, ::trySend))
    invokeOnClose { setOnDateChangeListener(null) }
}

/**
 * Create a flow which emits on [date change events][CalendarViewDateChangeEvent] on [CalendarView].
 *
 * *Warning:* The created flow uses [CalendarView.setOnDateChangeListener]. Only one flow can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * calendarView.dateChangeEvents()
 *      .onEach { /* handle date change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * calendarView.dateChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle date change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun CalendarView.dateChangeEvents(): InitialValueFlow<CalendarViewDateChangeEvent> = channelFlow {
    setOnDateChangeListener(listener(this, ::trySend))
    awaitClose { setOnDateChangeListener(null) }
}.asInitialValueFlow(initialValue(calendar = this))

@CheckResult
private fun initialValue(calendar: CalendarView): CalendarViewDateChangeEvent =
    with(Calendar.getInstance()) {
        val year = get(Calendar.YEAR)
        val month = get(Calendar.MONTH)
        val dayOfMonth = get(Calendar.DAY_OF_MONTH)
        CalendarViewDateChangeEvent(calendar, year, month, dayOfMonth)
    }

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (CalendarViewDateChangeEvent) -> Unit
) = CalendarView.OnDateChangeListener { view, year, month, dayOfMonth ->
    if (scope.isActive) { emitter(CalendarViewDateChangeEvent(view, year, month, dayOfMonth)) }
}

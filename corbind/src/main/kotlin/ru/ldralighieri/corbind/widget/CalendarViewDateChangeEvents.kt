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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement
import java.util.Calendar

data class CalendarViewDateChangeEvent(
    val view: CalendarView,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int
)

/**
 * Perform an action on [CalendarView] date change events.
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
    val events = scope.actor<CalendarViewDateChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    setOnDateChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnDateChangeListener(null) }
}

/**
 * Perform an action on [CalendarView] date change events, inside new [CoroutineScope].
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
 * Create a channel which emits on [CalendarView] date change events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun CalendarView.dateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<CalendarViewDateChangeEvent> = corbindReceiveChannel(capacity) {
    offerElement(initialValue(this@dateChangeEvents))
    setOnDateChangeListener(listener(scope, ::offerElement))
    invokeOnClose { setOnDateChangeListener(null) }
}

/**
 * Create a flow which emits on [CalendarView] date change events.
 */
@CheckResult
fun CalendarView.dateChangeEvents(): Flow<CalendarViewDateChangeEvent> = channelFlow {
    offer(initialValue(this@dateChangeEvents))
    setOnDateChangeListener(listener(this, ::offer))
    awaitClose { setOnDateChangeListener(null) }
}

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
    emitter: (CalendarViewDateChangeEvent) -> Boolean
) = CalendarView.OnDateChangeListener { view, year, month, dayOfMonth ->
    if (scope.isActive) { emitter(CalendarViewDateChangeEvent(view, year, month, dayOfMonth)) }
}

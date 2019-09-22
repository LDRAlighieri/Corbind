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

package ru.ldralighieri.corbind.app

import android.app.DatePickerDialog
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel

data class DatePickerDialogSetEvent(
    val view: DatePicker,
    val year: Int,
    val monthOfYear: Int,
    val dayOfMonth: Int
)

/**
 * Perform an action on [date set events][DatePickerDialogSetEvent] on [DatePickerDialog].
 *
 * *Warning:* The created actor uses [DatePickerDialog.setOnDateSetListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.N)
fun DatePickerDialog.dateSetEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (DatePickerDialogSetEvent) -> Unit
) {
    val events = scope.actor<DatePickerDialogSetEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnDateSetListener(listener(scope, events::offer))
    events.invokeOnClose { setOnDateSetListener(null) }
}

/**
 * Perform an action on [date set events][DatePickerDialogSetEvent] on [DatePickerDialog], inside
 * new [CoroutineScope].
 *
 * *Warning:* The created actor uses [DatePickerDialog.setOnDateSetListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun DatePickerDialog.dateSetEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (DatePickerDialogSetEvent) -> Unit
) = coroutineScope {
    dateSetEvents(this, capacity, action)
}

/**
 * Create a channel which emits on [date set events][DatePickerDialogSetEvent] on
 * [DatePickerDialog].
 *
 * *Warning:* The created channel uses [DatePickerDialog.setOnDateSetListener]. Only one channel can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      datePickerDialog.dateSetEvents(scope)
 *          .consumeEach { /* handle date set event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.N)
@CheckResult
fun DatePickerDialog.dateSetEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<DatePickerDialogSetEvent> = corbindReceiveChannel(capacity) {
    setOnDateSetListener(listener(scope, ::offer))
    invokeOnClose { setOnDateSetListener(null) }
}

/**
 * Create a flow which emits on [date set events][DatePickerDialogSetEvent] on [DatePickerDialog].
 *
 * *Warning:* The created flow uses [DatePickerDialog.setOnDateSetListener]. Only one flow can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * datePickerDialog.dateSetEvents()
 *      .onEach { /* handle date set event */ }
 *      .launchIn(scope)
 * ```
 */
@RequiresApi(Build.VERSION_CODES.N)
@CheckResult
fun DatePickerDialog.dateSetEvents(): Flow<DatePickerDialogSetEvent> = channelFlow {
    setOnDateSetListener(listener(this, ::offer))
    awaitClose { setOnDateSetListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (DatePickerDialogSetEvent) -> Boolean
) = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
    if (scope.isActive) { emitter(DatePickerDialogSetEvent(view, year, month, dayOfMonth)) }
}

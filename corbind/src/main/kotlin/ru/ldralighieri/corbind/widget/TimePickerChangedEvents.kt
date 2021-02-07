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
import android.widget.TimePicker
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
import ru.ldralighieri.corbind.internal.offerCatching

data class TimeChangedEvent(
    val view: TimePicker,
    val hourOfDay: Int,
    val minute: Int
)

/**
 * Perform an action on [time changed events][TimeChangedEvent] on [TimePicker].
 *
 * *Warning:* The created actor uses [TimePicker.setOnTimeChangedListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.M)
fun TimePicker.timeChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TimeChangedEvent) -> Unit
) {
    val events = scope.actor<TimeChangedEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.offer(TimeChangedEvent(this, hour, minute))
    setOnTimeChangedListener(listener(scope, events::offer))
    events.invokeOnClose { setOnTimeChangedListener(null) }
}

/**
 * Perform an action on [time changed events][TimeChangedEvent] on [TimePicker], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [TimePicker.setOnTimeChangedListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.M)
suspend fun TimePicker.timeChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (TimeChangedEvent) -> Unit
) = coroutineScope {
    timeChangeEvents(this, capacity, action)
}

/**
 * Create a channel which emits on [time changed events][TimeChangedEvent] on [TimePicker].
 *
 * *Warning:* The created channel uses [TimePicker.setOnTimeChangedListener]. Only one channel can
 * be used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      timePicker.timeChangeEvents(scope)
 *          .consumeEach { /* handle time changed event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.M)
@CheckResult
fun TimePicker.timeChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TimeChangedEvent> = corbindReceiveChannel(capacity) {
    offerCatching(TimeChangedEvent(this@timeChangeEvents, hour, minute))
    setOnTimeChangedListener(listener(scope, ::offerCatching))
    invokeOnClose { setOnTimeChangedListener(null) }
}

/**
 * Create a flow which emits on [time changed events][TimeChangedEvent] on [TimePicker].
 *
 * *Warning:* The created flow uses [TimePicker.setOnTimeChangedListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * timePicker.timeChangeEvents()
 *      .onEach { /* handle time changed event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * timePicker.timeChangeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle time changed event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@RequiresApi(Build.VERSION_CODES.M)
@CheckResult
fun TimePicker.timeChangeEvents(): InitialValueFlow<TimeChangedEvent> =
    channelFlow<TimeChangedEvent> {
        setOnTimeChangedListener(listener(this, ::offerCatching))
        awaitClose { setOnTimeChangedListener(null) }
    }.asInitialValueFlow(TimeChangedEvent(view = this, hour, minute))

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (TimeChangedEvent) -> Boolean
) = TimePicker.OnTimeChangedListener { view, hourOfDay, minute ->
    if (scope.isActive) { emitter(TimeChangedEvent(view, hourOfDay, minute)) }
}

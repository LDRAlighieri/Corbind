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

import android.widget.SeekBar
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

sealed class SeekBarChangeEvent {
    abstract val view: SeekBar
}

data class SeekBarProgressChangeEvent(
    override val view: SeekBar,
    val progress: Int,
    val fromUser: Boolean
) : SeekBarChangeEvent()

data class SeekBarStartChangeEvent(
    override val view: SeekBar
) : SeekBarChangeEvent()

data class SeekBarStopChangeEvent(
    override val view: SeekBar
) : SeekBarChangeEvent()

/**
 * Perform an action on [change events][SeekBarChangeEvent] for [SeekBar].
 *
 * *Warning:* The created actor uses [SeekBar.OnSeekBarChangeListener]. Only one actor can be used
 * at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.changeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SeekBarChangeEvent) -> Unit
) {
    val events = scope.actor<SeekBarChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    setOnSeekBarChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}

/**
 * Perform an action on [change events][SeekBarChangeEvent] for [SeekBar], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SeekBar.OnSeekBarChangeListener]. Only one actor can be used
 * at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.changeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SeekBarChangeEvent) -> Unit
) = coroutineScope {
    changeEvents(this, capacity, action)
}

/**
 * Create a channel of [change events][SeekBarChangeEvent] for [SeekBar].
 *
 * *Warning:* The created channel uses [SeekBar.OnSeekBarChangeListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      seekBar.changeEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is SeekBarProgressChangeEvent -> { /* handle progress change event */ }
 *                  is SeekBarStartChangeEvent -> { /* handle start change event */ }
 *                  is SeekBarStopChangeEvent -> { /* handle stop change event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      seekBar.changeEvents(scope)
 *          .filterIsInstance<SeekBarProgressChangeEvent>()
 *          .consumeEach { /* handle progress change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.changeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SeekBarChangeEvent> = corbindReceiveChannel(capacity) {
    offerElement(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(scope, ::offerElement))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}

/**
 * Create a flow of [change events][SeekBarChangeEvent] for [SeekBar].
 *
 * *Warning:* The created flow uses [SeekBar.OnSeekBarChangeListener]. Only one flow can be used at
 * a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * seekBar.changeEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is SeekBarProgressChangeEvent -> { /* handle progress change event */ }
 *              is SeekBarStartChangeEvent -> { /* handle start change event */ }
 *              is SeekBarStopChangeEvent -> { /* handle stop change event */ }
 *          }
 *      }
 *      .launchIn(scope)
 *
 * // handle one event
 * seekBar.changeEvents()
 *      .filterIsInstance<SeekBarProgressChangeEvent>()
 *      .onEach { /* handle progress change event */ }
 *      .launchIn(scope)
 *
 * // drop one event
 * seekBar.changeEvents()
 *      .drop(1)
 *      .onEach { /* handle event */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun SeekBar.changeEvents(): Flow<SeekBarChangeEvent> = channelFlow {
    offer(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(this, ::offer))
    awaitClose { setOnSeekBarChangeListener(null) }
}

@CheckResult
private fun initialValue(seekBar: SeekBar): SeekBarChangeEvent =
        SeekBarProgressChangeEvent(seekBar, seekBar.progress, false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (SeekBarChangeEvent) -> Boolean
) = object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        onEvent(SeekBarProgressChangeEvent(seekBar, progress, fromUser))
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        onEvent(SeekBarStartChangeEvent(seekBar))
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        onEvent(SeekBarStopChangeEvent(seekBar))
    }

    private fun onEvent(event: SeekBarChangeEvent) {
        if (scope.isActive) { emitter(event) }
    }
}

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
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

sealed interface SeekBarChangeEvent {
    val view: SeekBar
}

data class SeekBarProgressChangeEvent(
    override val view: SeekBar,
    val progress: Int,
    val fromUser: Boolean,
) : SeekBarChangeEvent

data class SeekBarStartChangeEvent(
    override val view: SeekBar,
) : SeekBarChangeEvent

data class SeekBarStopChangeEvent(
    override val view: SeekBar,
) : SeekBarChangeEvent

/**
 * Perform an action on [change events][SeekBarChangeEvent] for [SeekBar].
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun SeekBar.changeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SeekBarChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<SeekBarChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    events.corbindEventEmitter(scope)(initialValue(this))
    setOnSeekBarChangeListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setOnSeekBarChangeListener(null) }
}

/**
 * Perform an action on [change events][SeekBarChangeEvent] for [SeekBar], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun SeekBar.changeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SeekBarChangeEvent) -> Unit,
) = coroutineScope {
    changeEvents(this, capacity, action)
}

/**
 * Create a channel of [change events][SeekBarChangeEvent] for [SeekBar].
 *
 * *Warning:* The created channel uses [SeekBar.setOnSeekBarChangeListener]. Only one channel can be
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
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun SeekBar.changeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<SeekBarChangeEvent> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(initialValue(this@changeEvents))
    setOnSeekBarChangeListener(listener(scope, corbindEventEmitter()))
    awaitClose { setOnSeekBarChangeListener(null) }
}

/**
 * Create a flow of [change events][SeekBarChangeEvent] for [SeekBar].
 *
 * *Warning:* The created flow uses [SeekBar.setOnSeekBarChangeListener]. Only one flow can be used
 * at a time.
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
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // handle one event
 * seekBar.changeEvents()
 *      .filterIsInstance<SeekBarProgressChangeEvent>()
 *      .onEach { /* handle progress change event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop one event
 * seekBar.changeEvents()
 *      .dropInitialValue()
 *      .onEach { /* handle event */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SeekBar.changeEvents(): InitialValueFlow<SeekBarChangeEvent> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    setOnSeekBarChangeListener(listener(this, emitter))
    emitter.sendInitialValue(initialValue(seekBar = this@changeEvents))
    awaitClose { setOnSeekBarChangeListener(null) }
}.asInitialValueFlow()

@CheckResult
private fun initialValue(seekBar: SeekBar): SeekBarChangeEvent = SeekBarProgressChangeEvent(seekBar, seekBar.progress, false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (SeekBarChangeEvent) -> Boolean,
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
        if (scope.isActive) emitter(event)
    }
}

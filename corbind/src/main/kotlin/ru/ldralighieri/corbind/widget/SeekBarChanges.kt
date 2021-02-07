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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerCatching

private fun SeekBar.changes(
    scope: CoroutineScope,
    capacity: Int,
    shouldBeFromUser: Boolean?,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (progress in channel) action(progress)
    }

    events.offer(progress)
    setOnSeekBarChangeListener(listener(scope, shouldBeFromUser, events::offer))
    events.invokeOnClose { setOnSeekBarChangeListener(null) }
}

private suspend fun SeekBar.changes(
    capacity: Int,
    shouldBeFromUser: Boolean?,
    action: suspend (Int) -> Unit
) = coroutineScope {
    changes(this, capacity, shouldBeFromUser, action)
}

@CheckResult
private fun SeekBar.changes(
    scope: CoroutineScope,
    capacity: Int,
    shouldBeFromUser: Boolean?
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    offerCatching(progress)
    setOnSeekBarChangeListener(listener(scope, shouldBeFromUser, ::offerCatching))
    invokeOnClose { setOnSeekBarChangeListener(null) }
}

@CheckResult
private fun SeekBar.changes(shouldBeFromUser: Boolean?): InitialValueFlow<Int> = channelFlow<Int> {
    setOnSeekBarChangeListener(listener(this, shouldBeFromUser, ::offerCatching))
    awaitClose { setOnSeekBarChangeListener(null) }
}.asInitialValueFlow(progress)

/**
 * Perform an action on progress value changes on [SeekBar].
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.changes(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = changes(scope, capacity, null, action)

/**
 * Perform an action on progress value changes on [SeekBar] inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.changes(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = changes(capacity, null, action)

/**
 * Create a channel of progress value changes on [SeekBar].
 *
 * *Warning:* The created channel uses [SeekBar.setOnSeekBarChangeListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      seekBar.changes(scope)
 *          .consumeEach { /* handle progress value change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.changes(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = changes(scope, capacity, null)

/**
 * Create a flow of progress value changes on [SeekBar].
 *
 * *Warning:* The created flow uses [SeekBar.setOnSeekBarChangeListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * seekBar.changes()
 *      .onEach { /* handle progress value change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * seekBar.changes()
 *      .dropInitialValue()
 *      .onEach { /* handle progress value change */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun SeekBar.changes(): InitialValueFlow<Int> = changes(null)

/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the user.
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.userChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = changes(scope, capacity, true, action)

/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the user inside
 * new [CoroutineScope].
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.userChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = changes(capacity, true, action)

/**
 * Create a channel of progress value changes on [SeekBar] that were made only from the user.
 *
 * *Warning:* The created channel uses [SeekBar.setOnSeekBarChangeListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      seekBar.userChanges(scope)
 *          .consumeEach { /* handle progress value change made from user */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.userChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = changes(scope, capacity, true)

/**
 * Create a flow of progress value changes on [SeekBar] that were made only from the user.
 *
 * *Warning:* The created flow uses [SeekBar.setOnSeekBarChangeListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * seekBar.userChanges()
 *      .onEach { /* handle progress value change made from user */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * seekBar.userChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle progress value change made from user */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun SeekBar.userChanges(): InitialValueFlow<Int> = changes(true)

/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the system.
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SeekBar.systemChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = changes(scope, capacity, false, action)

/**
 * Perform an action on progress value changes on [SeekBar] that were made only from the system inside
 * new [CoroutineScope].
 *
 * *Warning:* The created actor uses [SeekBar.setOnSeekBarChangeListener]. Only one actor can be
 * used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SeekBar.systemChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = changes(capacity, false, action)

/**
 * Create a channel of progress value changes on [SeekBar] that were made only from the system.
 *
 * *Warning:* The created channel uses [SeekBar.setOnSeekBarChangeListener]. Only one channel can be
 * used at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      seekBar.systemChanges(scope)
 *          .consumeEach { /* handle progress value change made from system */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SeekBar.systemChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = changes(scope, capacity, false)

/**
 * Create a flow of progress value changes on [SeekBar] that were made only from the system.
 *
 * *Warning:* The created flow uses [SeekBar.setOnSeekBarChangeListener]. Only one flow can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * seekBar.systemChanges()
 *      .onEach { /* handle progress value change made from system */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * seekBar.systemChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle progress value change made from system */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun SeekBar.systemChanges(): InitialValueFlow<Int> = changes(false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    shouldBeFromUser: Boolean?,
    emitter: (Int) -> Boolean
) = object : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (scope.isActive && (shouldBeFromUser == null || shouldBeFromUser == fromUser)) {
            emitter(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
}

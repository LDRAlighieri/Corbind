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

package ru.ldralighieri.corbind.view

import android.view.View
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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on [View] focus change.
 *
 * *Warning:* The created actor uses [View.setOnFocusChangeListener]. Only one actor can be used at
 * a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.focusChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main.immediate, capacity) {
        for (focus in channel) action(focus)
    }

    events.offer(hasFocus())
    onFocusChangeListener = listener(scope, events::offer)
    events.invokeOnClose { onFocusChangeListener = null }
}

/**
 * Perform an action on [View] focus change, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [View.setOnFocusChangeListener]. Only one actor can be used at
 * a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.focusChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    focusChanges(this, capacity, action)
}

/**
 * Create a channel of booleans representing the focus of [View].
 *
 * *Warning:* The created channel uses [View.setOnFocusChangeListener]. Only one channel can be used
 * at a time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      datePickerDialog.dateSetEvents(scope)
 *          .consumeEach { /* handle focus change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.focusChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    offerCatching(hasFocus())
    onFocusChangeListener = listener(scope, ::offerCatching)
    invokeOnClose { onFocusChangeListener = null }
}

/**
 * Create a flow of booleans representing the focus of [View].
 *
 * *Warning:* The created flow uses [View.setOnFocusChangeListener]. Only one flow can be used at a
 * time.
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * view.focusChanges()
 *      .onEach { /* handle focus change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * view.focusChanges()
 *      .drop(1)
 *      .onEach { /* handle focus change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun View.focusChanges(): Flow<Boolean> = channelFlow {
    offer(hasFocus())
    onFocusChangeListener = listener(this, ::offerCatching)
    awaitClose { onFocusChangeListener = null }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean
) = View.OnFocusChangeListener { _, hasFocus ->
    if (scope.isActive) { emitter(hasFocus) }
}

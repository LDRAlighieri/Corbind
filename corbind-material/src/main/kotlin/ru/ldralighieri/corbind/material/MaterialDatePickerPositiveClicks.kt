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

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
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
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on [MaterialDatePicker] positive button click.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <S> MaterialDatePicker<S>.positiveClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (S) -> Unit
) {
    val events = scope.actor<S>(Dispatchers.Main, capacity) {
        for (selection in channel) action(selection)
    }

    val listener = listener(scope, events::offer)
    addOnPositiveButtonClickListener(listener)
    events.invokeOnClose { removeOnPositiveButtonClickListener(listener) }
}

/**
 * Perform an action on [MaterialDatePicker] positive button click, inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <S> MaterialDatePicker<S>.positiveClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (S) -> Unit
) = coroutineScope {
    positiveClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [MaterialDatePicker] positive button click.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialDatePicker.positiveClicks(scope)
 *          .consumeEach { /* handle positive button click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <S> MaterialDatePicker<S>.positiveClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<S> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnPositiveButtonClickListener(listener)
    invokeOnClose { removeOnPositiveButtonClickListener(listener) }
}

/**
 * Create a flow which emits [MaterialDatePicker] positive button click.
 *
 * Example:
 *
 * ```
 * materialDatePicker.positiveClicks()
 *      .onEach { /* handle positive button click */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun <S> MaterialDatePicker<S>.positiveClicks(): Flow<S> = channelFlow {
    val listener = listener(this, ::offer)
    addOnPositiveButtonClickListener(listener)
    awaitClose { removeOnPositiveButtonClickListener(listener) }
}

@CheckResult
private fun <S> listener(
    scope: CoroutineScope,
    emitter: (S) -> Boolean
) = MaterialPickerOnPositiveButtonClickListener<S> { selection ->
    if (scope.isActive) { emitter(selection) }
}

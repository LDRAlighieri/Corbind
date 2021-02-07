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

import android.view.View
import androidx.annotation.CheckResult
import com.google.android.material.datepicker.MaterialDatePicker
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
 * Perform an action on [MaterialDatePicker] negative button click.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <S> MaterialDatePicker<S>.negativeClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, events::offer)
    addOnNegativeButtonClickListener(listener)
    events.invokeOnClose { removeOnNegativeButtonClickListener(listener) }
}

/**
 * Perform an action on [MaterialDatePicker] negative button click, inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <S> MaterialDatePicker<S>.negativeClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    negativeClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [MaterialDatePicker] negative button click.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialDatePicker.negativeClicks(scope)
 *          .consumeEach { /* handle negative button click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <S> MaterialDatePicker<S>.negativeClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::offerCatching)
    addOnNegativeButtonClickListener(listener)
    invokeOnClose { removeOnNegativeButtonClickListener(listener) }
}

/**
 * Create a flow which emits on [MaterialDatePicker] negative button click.
 *
 * Example:
 *
 * ```
 * materialDatePicker.negativeClicks()
 *      .onEach { /* handle negative button click */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <S> MaterialDatePicker<S>.negativeClicks(): Flow<Unit> = channelFlow<Unit> {
    val listener = listener(this, ::offerCatching)
    addOnNegativeButtonClickListener(listener)
    awaitClose { removeOnNegativeButtonClickListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = View.OnClickListener {
    if (scope.isActive) { emitter(Unit) }
}

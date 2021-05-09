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

import android.content.DialogInterface
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

/**
 * Perform an action when the user cancels the [MaterialDatePicker] via back button or a touch
 * outside the view.
 *
 * *Note:* It is not called when the user clicks the cancel button. To add a listener for use when
 * the user clicks the cancel button, use `negativeClicks` extension instead.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <S> MaterialDatePicker<S>.cancels(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, events::trySend)
    addOnCancelListener(listener)
    events.invokeOnClose { removeOnCancelListener(listener) }
}

/**
 * Perform an action when the user cancels the [MaterialDatePicker] via back button or a touch
 * outside the view, inside new [CoroutineScope].
 *
 * *Note:* It is not called when the user clicks the cancel button. To add a listener for use when
 * the user clicks the cancel button, use `negativeClicks` extension instead.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <S> MaterialDatePicker<S>.cancels(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    cancels(this, capacity, action)
}

/**
 * Create a channel which emits when the user cancels the [MaterialDatePicker] via back button or a
 * touch outside the view.
 *
 * *Note:* It is not called when the user clicks the cancel button. To add a listener for use when
 * the user clicks the cancel button, use `negativeClicks` extension instead.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialDatePicker.cancels(scope)
 *          .consumeEach { /* handle cancel event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <S> MaterialDatePicker<S>.cancels(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addOnCancelListener(listener)
    invokeOnClose { removeOnCancelListener(listener) }
}

/**
 * Create a flow which emits when the user cancels the [MaterialDatePicker] via back button or a
 * touch outside the view.
 *
 * *Note:* It is not called when the user clicks the cancel button. To add a listener for use when
 * the user clicks the cancel button, use `negativeClicks` extension instead.
 *
 * Example:
 *
 * ```
 * materialDatePicker.cancels()
 *      .onEach { /* handle cancel event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <S> MaterialDatePicker<S>.cancels(): Flow<Unit> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnCancelListener(listener)
    awaitClose { removeOnCancelListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Unit
) = DialogInterface.OnCancelListener {
    if (scope.isActive) { emitter(Unit) }
}

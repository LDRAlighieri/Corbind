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
 * Perform an action whenever the [MaterialDatePicker] is dismissed, no matter how it is dismissed.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun <S> MaterialDatePicker<S>.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, events::trySend)
    addOnDismissListener(listener)
    events.invokeOnClose { removeOnDismissListener(listener) }
}

/**
 * Perform an action whenever the [MaterialDatePicker] is dismissed, no matter how it is dismissed.
 * Inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun <S> MaterialDatePicker<S>.dismisses(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    dismisses(this, capacity, action)
}

/**
 * Create a channel which emits whenever the [MaterialDatePicker] is dismissed, no matter how it is
 * dismissed.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialDatePicker.dismisses(scope)
 *          .consumeEach { /* handle dismiss */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun <S> MaterialDatePicker<S>.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::trySend)
    addOnDismissListener(listener)
    invokeOnClose { removeOnDismissListener(listener) }
}

/**
 * Create a flow which emits whenever the [MaterialDatePicker] is dismissed, no matter how it is
 * dismissed.
 *
 * Example:
 *
 * ```
 * materialDatePicker.dismisses()
 *      .onEach { /* handle dismiss */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <S> MaterialDatePicker<S>.dismisses(): Flow<Unit> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnDismissListener(listener)
    awaitClose { removeOnDismissListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Unit,
) = DialogInterface.OnDismissListener {
    if (scope.isActive) emitter(Unit)
}

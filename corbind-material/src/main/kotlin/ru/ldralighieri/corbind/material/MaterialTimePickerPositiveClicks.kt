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
import com.google.android.material.timepicker.MaterialTimePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

/**
 * Perform an action on [MaterialTimePicker] positive button click.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
fun MaterialTimePicker.positiveClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnPositiveButtonClickListener(listener)
    events.invokeOnClose { removeOnPositiveButtonClickListener(listener) }
}

/**
 * Perform an action on [MaterialTimePicker] positive button click, inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
suspend fun MaterialTimePicker.positiveClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    positiveClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [MaterialTimePicker] positive button click.
 *
 * Example:
 *
 * ```
 * launch {
 *      materialTimePicker.positiveClicks(scope)
 *          .consumeEach { /* handle positive button click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun MaterialTimePicker.positiveClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addOnPositiveButtonClickListener(listener)
    awaitClose { removeOnPositiveButtonClickListener(listener) }
}

/**
 * Create a flow which emits on [MaterialTimePicker] positive button click.
 *
 * Example:
 *
 * ```
 * materialTimePicker.positiveClicks()
 *      .onEach { /* handle positive button click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun MaterialTimePicker.positiveClicks(): Flow<Unit> = callbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addOnPositiveButtonClickListener(listener)
    awaitClose { removeOnPositiveButtonClickListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean,
) = View.OnClickListener {
    if (scope.isActive) emitter(Unit)
}

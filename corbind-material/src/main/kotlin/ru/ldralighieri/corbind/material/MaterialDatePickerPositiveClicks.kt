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
import androidx.annotation.MainThread
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
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action on [MaterialDatePicker] positive button clicks.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun <S> MaterialDatePicker<S>.positiveClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (S) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<S>(Dispatchers.Main.immediate, capacity) {
        for (selection in channel) action(selection)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnPositiveButtonClickListener(listener)
    events.invokeOnCloseOnMain { removeOnPositiveButtonClickListener(listener) }
}

/**
 * Perform an action on [MaterialDatePicker] positive button clicks, in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun <S> MaterialDatePicker<S>.positiveClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (S) -> Unit,
) = coroutineScope {
    positiveClicks(this, capacity, action)
}

/**
 * Create a channel that emits [MaterialDatePicker] positive button clicks.
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun <S> MaterialDatePicker<S>.positiveClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<S> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addOnPositiveButtonClickListener(listener)
    awaitClose { removeOnPositiveButtonClickListener(listener) }
}

/**
 * Create a flow that emits [MaterialDatePicker] positive button clicks.
 *
 * Example:
 *
 * ```
 * materialDatePicker.positiveClicks()
 *      .onEach { /* handle positive button click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun <S> MaterialDatePicker<S>.positiveClicks(): Flow<S> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addOnPositiveButtonClickListener(listener)
    awaitClose { removeOnPositiveButtonClickListener(listener) }
}

@CheckResult
private fun <S> listener(
    scope: CoroutineScope,
    emitter: (S) -> Boolean,
) = MaterialPickerOnPositiveButtonClickListener<S> { selection ->
    if (scope.isActive) emitter(selection)
}

/*
 * Copyright 2020 Vladimir Raupov
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
import com.google.android.material.slider.Slider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.InitialValueFlow
import ru.ldralighieri.corbind.internal.asInitialValueFlow
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.initialValueFlowEmitter
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain
import ru.ldralighieri.corbind.internal.sendInitialValue

/**
 * Perform an action on value changes on [Slider].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun Slider.valueChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (value in channel) action(value)
    }

    events.corbindEventEmitter(scope)(value)
    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnChangeListener(listener)
    events.invokeOnCloseOnMain { removeOnChangeListener(listener) }
}

/**
 * Perform an action on value changes on [Slider], in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun Slider.valueChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit,
) = coroutineScope {
    valueChanges(this, capacity, action)
}

/**
 * Create a channel of the value changes on [Slider].
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Example:
 *
 * ```
 * launch {
 *      slider.valueChanges(scope)
 *          .consumeEach { /* handle value change */ }
 * }
 * ```
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun Slider.valueChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Float> = corbindReceiveChannel(scope, capacity) {
    sendInitialValue(value)
    val listener = listener(scope, corbindEventEmitter())
    addOnChangeListener(listener)
    awaitClose { removeOnChangeListener(listener) }
}

/**
 * Create a flow of the value changes on [Slider].
 *
 * *Note:* An initial value is emitted before subsequent events.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * slider.valueChanges()
 *      .onEach { /* handle value change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * slider.valueChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle value change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun Slider.valueChanges(): InitialValueFlow<Float> = corbindCallbackFlow {
    val emitter = initialValueFlowEmitter()
    val listener = listener(this, emitter)
    addOnChangeListener(listener)
    emitter.sendInitialValue(value)
    awaitClose { removeOnChangeListener(listener) }
}.asInitialValueFlow()

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Float) -> Boolean,
) = Slider.OnChangeListener { _, value, _ ->
    if (scope.isActive) emitter(value)
}

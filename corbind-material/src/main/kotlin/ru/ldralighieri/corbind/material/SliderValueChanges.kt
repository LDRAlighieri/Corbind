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
import com.google.android.material.slider.Slider
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

/**
 * Perform an action on value changes on [Slider].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun Slider.valueChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) {
    val events = scope.actor<Float>(Dispatchers.Main.immediate, capacity) {
        for (value in channel) action(value)
    }

    events.trySend(value)
    val listener = listener(scope, events::trySend)
    addOnChangeListener(listener)
    events.invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Perform an action on value changes on [Slider], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun Slider.valueChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Float) -> Unit
) = coroutineScope {
    valueChanges(this, capacity, action)
}

/**
 * Create a channel of the value changes on [Slider].
 *
 * *Note:* A value will be emitted immediately.
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
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun Slider.valueChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Float> = corbindReceiveChannel(capacity) {
    trySend(value)
    val listener = listener(scope, ::trySend)
    addOnChangeListener(listener)
    invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Create a flow of the value changes on [Slider].
 *
 * *Note:* A value will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial value
 * slider.valueChanges()
 *      .onEach { /* handle value change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial value
 * slider.valueChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle value change */ }
 *      .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
fun Slider.valueChanges(): InitialValueFlow<Float> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnChangeListener(listener)
    awaitClose { removeOnChangeListener(listener) }
}.asInitialValueFlow(value)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Float) -> Unit
) = Slider.OnChangeListener { _, value, _ ->
    if (scope.isActive) { emitter(value) }
}

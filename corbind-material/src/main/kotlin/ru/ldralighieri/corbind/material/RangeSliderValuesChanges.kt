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
import com.google.android.material.slider.RangeSlider
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
 * Perform an action on values changes on [RangeSlider].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RangeSlider.valuesChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (List<Float>) -> Unit
) {
    val events = scope.actor<List<Float>>(Dispatchers.Main.immediate, capacity) {
        for (values in channel) action(values)
    }

    events.trySend(values)
    val listener = listener(scope, events::trySend)
    addOnChangeListener(listener)
    events.invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Perform an action on rating changes on [RangeSlider], inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RangeSlider.valuesChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (List<Float>) -> Unit
) = coroutineScope {
    valuesChanges(this, capacity, action)
}

/**
 * Create a channel of the values changes on [RangeSlider].
 *
 * *Note:* A values will be emitted immediately.
 *
 * Example:
 *
 * ```
 * launch {
 *      slider.valuesChanges(scope)
 *          .consumeEach { /* handle values change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RangeSlider.valuesChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<List<Float>> = corbindReceiveChannel(capacity) {
    trySend(values)
    val listener = listener(scope, ::trySend)
    addOnChangeListener(listener)
    invokeOnClose { removeOnChangeListener(listener) }
}

/**
 * Create a flow of the values changes on [RangeSlider].
 *
 * *Note:* A values will be emitted immediately.
 *
 * Examples:
 *
 * ```
 * // handle initial values
 * slider.valuesChanges()
 *      .onEach { /* handle values change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // drop initial values
 * slider.valuesChanges()
 *      .dropInitialValue()
 *      .onEach { /* handle values change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun RangeSlider.valuesChanges(): InitialValueFlow<List<Float>> = channelFlow {
    val listener = listener(this, ::trySend)
    addOnChangeListener(listener)
    awaitClose { removeOnChangeListener(listener) }
}.asInitialValueFlow(values)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (List<Float>) -> Unit
) = RangeSlider.OnChangeListener { slider, _, _ ->
    if (scope.isActive) { emitter(slider.values) }
}

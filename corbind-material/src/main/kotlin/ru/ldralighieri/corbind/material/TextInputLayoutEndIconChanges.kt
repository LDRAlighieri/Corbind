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
import com.google.android.material.textfield.TextInputLayout
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
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on [TextInputLayout] end icon mode changes.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TextInputLayout.endIconChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (mode in channel) action(mode)
    }

    val listener = listener(scope, events::offer)
    addOnEndIconChangedListener(listener)
    events.invokeOnClose { removeOnEndIconChangedListener(listener) }
}

/**
 * Perform an action on [TextInputLayout] end icon mode changes, inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TextInputLayout.endIconChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    endIconChanges(this, capacity, action)
}

/**
 * Create a channel which emits on [TextInputLayout] end icon mode changes.
 *
 * *Note:* Emitted value is the [TextInputLayout.EndIconMode] the [TextInputLayout] previously had
 * set
 *
 * Example:
 *
 * ```
 * launch {
 *      textInputLayout.endIconChanges(scope)
 *          .consumeEach { /* handle end icon mode change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TextInputLayout.endIconChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::offerElement)
    addOnEndIconChangedListener(listener)
    invokeOnClose { removeOnEndIconChangedListener(listener) }
}

/**
 * Create a flow which emits on [TextInputLayout] end icon mode changes.
 *
 * *Note:* Emitted value is the [TextInputLayout.EndIconMode] the [TextInputLayout] previously had
 * set
 *
 * Example:
 *
 * ```
 * textInputLayout.endIconChanges()
 *      .onEach { /* handle end icon mode change */ }
 *      .launchIn(scope)
 * ```
 */
@CheckResult
fun TextInputLayout.endIconChanges(): Flow<Int> = channelFlow {
    val listener = listener(this, ::offer)
    addOnEndIconChangedListener(listener)
    awaitClose { removeOnEndIconChangedListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = TextInputLayout.OnEndIconChangedListener { _, previousIcon ->
    if (scope.isActive) { emitter(previousIcon) }
}

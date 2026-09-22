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
import com.google.android.material.textfield.TextInputLayout
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
 * Perform an action on [TextInputLayout] end icon mode changes.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun TextInputLayout.endIconChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (mode in channel) action(mode)
    }

    val listener = listener(scope, events.corbindEventEmitter(scope))
    addOnEndIconChangedListener(listener)
    events.invokeOnCloseOnMain { removeOnEndIconChangedListener(listener) }
}

/**
 * Perform an action on [TextInputLayout] end icon mode changes, in a new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun TextInputLayout.endIconChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    endIconChanges(this, capacity, action)
}

/**
 * Create a channel that emits [TextInputLayout] end icon mode changes.
 *
 * *Note:* The emitted value is the previous [TextInputLayout.EndIconMode].
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
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun TextInputLayout.endIconChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    val listener = listener(scope, corbindEventEmitter())
    addOnEndIconChangedListener(listener)
    awaitClose { removeOnEndIconChangedListener(listener) }
}

/**
 * Create a flow that emits [TextInputLayout] end icon mode changes.
 *
 * *Note:* The emitted value is the previous [TextInputLayout.EndIconMode].
 *
 * Example:
 *
 * ```
 * textInputLayout.endIconChanges()
 *      .onEach { /* handle end icon mode change */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun TextInputLayout.endIconChanges(): Flow<Int> = corbindCallbackFlow {
    val listener = listener(this, corbindEventEmitter())
    addOnEndIconChangedListener(listener)
    awaitClose { removeOnEndIconChangedListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = TextInputLayout.OnEndIconChangedListener { _, previousIcon ->
    if (scope.isActive) emitter(previousIcon)
}

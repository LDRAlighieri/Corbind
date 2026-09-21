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
 * Perform an action on [TextInputLayout] end icon click events.
 *
 * *Warning:* The created actor uses [TextInputLayout.setEndIconOnClickListener]. Only one actor
 * can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun TextInputLayout.endIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setEndIconOnClickListener(listener(scope, events.corbindEventEmitter(scope)))
    events.invokeOnCloseOnMain { setEndIconOnClickListener(null) }
}

/**
 * Perform an action on [TextInputLayout] end icon click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [TextInputLayout.setEndIconOnClickListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
suspend fun TextInputLayout.endIconClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    endIconClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [TextInputLayout] end icon click events.
 *
 * *Warning:* The created channel uses [TextInputLayout.setEndIconOnClickListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      textInputLayout.endIconClicks(scope)
 *          .consumeEach { /* handle end icon click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun TextInputLayout.endIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(scope, capacity) {
    setEndIconOnClickListener(listener(scope, corbindEventEmitter()))
    awaitClose { setEndIconOnClickListener(null) }
}

/**
 * Create a flow which emits [TextInputLayout] end icon click events.
 *
 * *Warning:* The created flow uses [TextInputLayout.setEndIconOnClickListener]. Only one flow can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * textInputLayout.endIconClicks()
 *      .onEach { /* handle end icon click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun TextInputLayout.endIconClicks(): Flow<Unit> = corbindCallbackFlow {
    setEndIconOnClickListener(listener(this, corbindEventEmitter()))
    awaitClose { setEndIconOnClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean,
) = View.OnClickListener {
    if (scope.isActive) emitter(Unit)
}

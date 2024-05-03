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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel

/**
 * Perform an action on [TextInputLayout] start icon click events.
 *
 * *Warning:* The created actor uses [TextInputLayout.setStartIconOnClickListener]. Only one actor
 * can be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TextInputLayout.startIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setStartIconOnClickListener(listener(scope, events::trySend))
    events.invokeOnClose { setStartIconOnClickListener(null) }
}

/**
 * Perform an action on [TextInputLayout] start icon click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [TextInputLayout.setStartIconOnClickListener]. Only one actor
 * can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TextInputLayout.startIconClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit,
) = coroutineScope {
    startIconClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [TextInputLayout] start icon click events.
 *
 * *Warning:* The created channel uses [TextInputLayout.setStartIconOnClickListener]. Only one
 * channel can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      textInputLayout.startIconClicks(scope)
 *          .consumeEach { /* handle start icon click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TextInputLayout.startIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setStartIconOnClickListener(listener(scope, ::trySend))
    invokeOnClose { setStartIconOnClickListener(null) }
}

/**
 * Create a flow which emits [TextInputLayout] start icon click events.
 *
 * *Warning:* The created flow uses [TextInputLayout.setStartIconOnClickListener]. Only one flow can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * textInputLayout.startIconClicks()
 *      .onEach { /* handle start icon click */ }
 *      .flowWithLifecycle(lifecycle)
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun TextInputLayout.startIconClicks(): Flow<Unit> = channelFlow {
    setStartIconOnClickListener(listener(this, ::trySend))
    awaitClose { setStartIconOnClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Unit,
) = View.OnClickListener {
    if (scope.isActive) emitter(Unit)
}

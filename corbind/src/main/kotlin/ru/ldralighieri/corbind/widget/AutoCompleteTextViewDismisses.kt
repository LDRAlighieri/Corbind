/*
 * Copyright 2021 Vladimir Raupov
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

package ru.ldralighieri.corbind.widget

import android.os.Build
import android.widget.AutoCompleteTextView
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
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
import ru.ldralighieri.corbind.internal.offerCatching

/**
 * Perform an action on [AutoCompleteTextView] dismiss events.
 *
 * *Warning:* The created actor uses [AutoCompleteTextView.setOnDismissListener]. Only one actor can
 * be used at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun AutoCompleteTextView.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setOnDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}

/**
 * Perform an action on [AutoCompleteTextView] dismiss events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [AutoCompleteTextView.setOnDismissListener]. Only one actor can
 * be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
suspend fun AutoCompleteTextView.dismisses(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    dismisses(this, capacity, action)
}

/**
 * Create a channel which emits on [AutoCompleteTextView] dismiss events
 *
 * *Warning:* The created channel uses [AutoCompleteTextView.setOnDismissListener]. Only one channel
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      autoCompleteTextView.dismisses(scope)
 *          .consumeEach { /* handle dismiss */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@CheckResult
fun AutoCompleteTextView.dismisses(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setOnDismissListener(listener(scope, ::offerCatching))
    invokeOnClose { setOnDismissListener(null) }
}

/**
 * Create a flow which emits on [AutoCompleteTextView] dismiss events
 *
 * *Warning:* The created flow uses [AutoCompleteTextView.setOnDismissListener]. Only one flow can
 * be used at a time.
 *
 * Example:
 *
 * ```
 * autoCompleteTextView.dismisses()
 *      .onEach { /* handle dismiss */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@CheckResult
fun AutoCompleteTextView.dismisses(): Flow<Unit> = channelFlow<Unit> {
    setOnDismissListener(listener(this, ::offerCatching))
    awaitClose { setOnDismissListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = AutoCompleteTextView.OnDismissListener {
    if (scope.isActive) { emitter(Unit) }
}

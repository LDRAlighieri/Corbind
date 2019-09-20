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
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.offerElement

/**
 * Perform an action on [TextInputLayout] end icon click events.
 *
 * *Warning:* The created actor uses [TextInputLayout.setEndIconOnClickListener] to emit clicks.
 * Only one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TextInputLayout.endIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    setEndIconOnClickListener(listener(scope, events::offer))
    events.invokeOnClose { setEndIconOnClickListener(null) }
}

/**
 * Perform an action on [TextInputLayout] end icon click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [TextInputLayout.setEndIconOnClickListener] to emit clicks.
 * Only one actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TextInputLayout.endIconClicks(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {
    endIconClicks(this, capacity, action)
}

/**
 * Create a channel which emits on [TextInputLayout] end icon click events.
 *
 * *Warning:* The created channel uses [TextInputLayout.setEndIconOnClickListener] to emit clicks.
 * Only one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TextInputLayout.endIconClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setEndIconOnClickListener(listener(scope, ::offerElement))
    invokeOnClose { setEndIconOnClickListener(null) }
}

/**
 * Create a flow which emits [TextInputLayout] end icon click events.
 *
 * *Warning:* The created flow uses [TextInputLayout.setEndIconOnClickListener] to emit clicks.
 * Only one flow can be used for a view at a time.
 */
@CheckResult
fun TextInputLayout.endIconClicks(): Flow<Unit> = channelFlow {
    setEndIconOnClickListener(listener(this, ::offer))
    awaitClose { setEndIconOnClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = View.OnClickListener {
    if (scope.isActive) { emitter(Unit) }
}

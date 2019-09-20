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
import com.google.android.material.button.MaterialButton
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
 * Perform an action on [MaterialButton] check state change.
 *
 * *Warning:* Perform only when the [MaterialButton] is in checkable state.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun MaterialButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, capacity) {
        for (checked in channel) action(checked)
    }

    checkCheckableState(this)
    events.offer(isChecked)
    val listener = listener(scope, events::offer)
    addOnCheckedChangeListener(listener)
    events.invokeOnClose { removeOnCheckedChangeListener(listener) }
}

/**
 * Perform an action on [MaterialButton] check state change, inside new [CoroutineScope].
 *
 * *Warning:* Perform only when the [MaterialButton] is in checkable state.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun MaterialButton.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel which emits on [MaterialButton] check state change.
 *
 * *Warning:* Emits only when the [MaterialButton] is in checkable state.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun MaterialButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    checkCheckableState(this@checkedChanges)
    offerElement(isChecked)
    val listener = listener(scope, ::offerElement)
    addOnCheckedChangeListener(listener)
    invokeOnClose { removeOnCheckedChangeListener(listener) }
}

/**
 * Create a flow which emits on [MaterialButton] check state change.
 *
 * *Warning:* Emits only when the [MaterialButton] is in checkable state.
 */
@CheckResult
fun MaterialButton.checkedChanges(): Flow<Boolean> = channelFlow {
    checkCheckableState(this@checkedChanges)
    offer(isChecked)
    val listener = listener(this, ::offer)
    addOnCheckedChangeListener(listener)
    awaitClose { removeOnCheckedChangeListener(listener) }
}

private fun checkCheckableState(button: MaterialButton) {
    check(button.isCheckable) { "The MaterialButton is not in checkable state" }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean
) = MaterialButton.OnCheckedChangeListener { _, isChecked ->
    if (scope.isActive) { emitter(isChecked) }
}

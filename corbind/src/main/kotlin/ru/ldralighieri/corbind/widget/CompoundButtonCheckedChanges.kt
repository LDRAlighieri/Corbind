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

package ru.ldralighieri.corbind.widget

import android.widget.CompoundButton
import androidx.annotation.CheckResult
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
 * Perform an action on checked state of [CompoundButton].
 *
 * *Warning:* The created actor uses [CompoundButton.setOnCheckedChangeListener] to emit checked
 * changes. Only one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun CompoundButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, capacity) {
        for (checked in channel) action(checked)
    }

    events.offer(isChecked)
    setOnCheckedChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked state of [CompoundButton], inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [CompoundButton.setOnCheckedChangeListener] to emit checked
 * changes. Only one actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun CompoundButton.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Boolean) -> Unit
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel of booleans representing the checked state of [CompoundButton].
 *
 * *Warning:* The created channel uses [CompoundButton.setOnCheckedChangeListener] to emit
 * checked changes. Only one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun CompoundButton.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {
    offer(isChecked)
    setOnCheckedChangeListener(listener(scope, ::offerElement))
    invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Create a flow of booleans representing the checked state of [CompoundButton].
 *
 * *Warning:* The created flow uses [CompoundButton.setOnCheckedChangeListener] to emit checked
 * changes. Only one flow can be used for a view at a time.
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun CompoundButton.checkedChanges(): Flow<Boolean> = channelFlow {
    offer(isChecked)
    setOnCheckedChangeListener(listener(this, ::offer))
    awaitClose { setOnCheckedChangeListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Boolean) -> Boolean
) = CompoundButton.OnCheckedChangeListener { _, isChecked ->
    if (scope.isActive) { emitter(isChecked) }
}

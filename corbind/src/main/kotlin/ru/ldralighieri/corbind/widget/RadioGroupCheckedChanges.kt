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

import android.view.View
import android.widget.RadioGroup
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
 * Perform an action on checked view ID changes in [RadioGroup].
 *
 * *Note:* When the selection is cleared, checkedId is [View.NO_ID]
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun RadioGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    events.offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Perform an action on checked view ID changes in [RadioGroup], inside new [CoroutineScope].
 *
 * *Note:* When the selection is cleared, checkedId is [View.NO_ID]
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun RadioGroup.checkedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    checkedChanges(this, capacity, action)
}

/**
 * Create a channel of the checked view ID changes in [RadioGroup].
 *
 * *Note:* When the selection is cleared, checkedId is [View.NO_ID]
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun RadioGroup.checkedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    offerElement(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(scope, ::offerElement))
    invokeOnClose { setOnCheckedChangeListener(null) }
}

/**
 * Create a flow of the checked view ID changes in [RadioGroup].
 *
 * *Note:* A value will be emitted immediately on collect. When the selection is cleared, checkedId
 * is [View.NO_ID]
 */
@CheckResult
fun RadioGroup.checkedChanges(): Flow<Int> = channelFlow {
    offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(this, ::offer))
    awaitClose { setOnCheckedChangeListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : RadioGroup.OnCheckedChangeListener {

    private var lastChecked = View.NO_ID
    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (scope.isActive && checkedId != lastChecked) {
            lastChecked = checkedId
            emitter(checkedId)
        }
    }
}

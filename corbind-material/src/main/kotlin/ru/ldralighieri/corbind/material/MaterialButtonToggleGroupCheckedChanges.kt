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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
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
 * Perform an action on [MaterialButton] check state change in [MaterialButtonToggleGroup].
 *
 * *Warning:* Only in single selection mode, use `buttonCheckedChangeEvents` extension instead.
 *
 * *Note:* The action is performed only on [MaterialButton] check events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun MaterialButtonToggleGroup.buttonCheckedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, capacity) {
        for (checkedId in channel) action(checkedId)
    }

    checkSelectionMode(this)
    events.offer(checkedButtonId)
    val listener = listener(scope, events::offer)
    addOnButtonCheckedListener(listener)
    events.invokeOnClose { removeOnButtonCheckedListener(listener) }
}

/**
 * Perform an action on [MaterialButton] check state change in [MaterialButtonToggleGroup], inside
 * new [CoroutineScope]
 *
 * *Warning:* Only in single selection mode, use `buttonCheckedChangeEvents` extension instead.
 *
 * *Note:* The action is performed only on [MaterialButton] check events.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun MaterialButtonToggleGroup.buttonCheckedChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit
) = coroutineScope {
    buttonCheckedChanges(this, capacity, action)
}

/**
 * Create a channel which emits on [MaterialButton] check state change in
 * [MaterialButtonToggleGroup]
 *
 * *Warning:* Only in single selection mode, use `buttonCheckedChanges` extension instead.
 *
 * *Note:* Channel emits only on [MaterialButton] check events. When the selection is
 * cleared, [View.NO_ID] will be emitted.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun MaterialButtonToggleGroup.buttonCheckedChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Int> = corbindReceiveChannel(capacity) {
    checkSelectionMode(this@buttonCheckedChanges)
    offerElement(checkedButtonId)
    val listener = listener(scope, ::offerElement)
    addOnButtonCheckedListener(listener)
    invokeOnClose { removeOnButtonCheckedListener(listener) }
}

/**
 * Create a flow which emits on [MaterialButton] check state change in
 * [MaterialButtonToggleGroup]
 *
 * *Warning:* Only in single selection mode, use `buttonCheckedChanges` extension instead.
 *
 * *Note:* Flow emits only on [MaterialButton] check events. When the selection is
 * cleared, [View.NO_ID] will be emitted.
 */
@CheckResult
fun MaterialButtonToggleGroup.buttonCheckedChanges(): Flow<Int> = channelFlow {
    checkSelectionMode(this@buttonCheckedChanges)
    offer(checkedButtonId)
    val listener = listener(this, ::offer)
    addOnButtonCheckedListener(listener)
    awaitClose { removeOnButtonCheckedListener(listener) }
}

private fun checkSelectionMode(group: MaterialButtonToggleGroup) {
    check(group.isSingleSelection) { "The MaterialButtonToggleGroup is not in single selection mode. " +
        "Use `buttonCheckedChangeEvents` extension instead" }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean
) = object : MaterialButtonToggleGroup.OnButtonCheckedListener {

    private var lastChecked = View.NO_ID
    override fun onButtonChecked(
        group: MaterialButtonToggleGroup,
        checkedId: Int,
        isChecked: Boolean
    ) {
        if (scope.isActive) {
            when {
                checkedId != lastChecked && isChecked -> {
                    lastChecked = checkedId
                    emitter(lastChecked)
                }
                checkedId == lastChecked && !isChecked -> {
                    lastChecked = View.NO_ID
                    emitter(lastChecked)
                }
            }
        }
    }
}

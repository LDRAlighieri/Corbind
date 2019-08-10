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

package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewTreeObserver
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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.offerElement

/**
 * Perform an action on [View] globalLayout events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.globalLayouts(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(scope, events::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

/**
 * Perform an action on [View] globalLayout events. inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.globalLayouts(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(this, events::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

/**
 * Create a channel which emits on [View] globalLayout events.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.globalLayouts(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::offerElement)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    invokeOnClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

/**
 * Create a flow which emits on [View] globalLayout events.
 */
@CheckResult
fun View.globalLayouts(): Flow<Unit> = channelFlow {
    val listener = listener(this, ::offer)
    viewTreeObserver.addOnGlobalLayoutListener(listener)
    awaitClose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnGlobalLayoutListener {
    if (scope.isActive) { emitter(Unit) }
}

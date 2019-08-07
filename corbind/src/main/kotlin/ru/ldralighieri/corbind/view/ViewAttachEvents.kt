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
import ru.ldralighieri.corbind.internal.safeOffer

sealed class ViewAttachEvent {
    abstract val view: View
}

data class ViewAttachAttachedEvent(
    override val view: View
) : ViewAttachEvent()

data class ViewAttachDetachedEvent(
    override val view: View
) : ViewAttachEvent()

/**
 * Perform an action on attach and detach events on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun View.attachEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewAttachEvent) -> Unit
) {

    val events = scope.actor<ViewAttachEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(scope, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Perform an action on attach and detach events on [View] inside new CoroutineScope.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun View.attachEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewAttachEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewAttachEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    val listener = listener(this, events::offer)
    addOnAttachStateChangeListener(listener)
    events.invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Create a channel of attach and detach events on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun View.attachEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewAttachEvent> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::safeOffer)
    addOnAttachStateChangeListener(listener)
    invokeOnClose { removeOnAttachStateChangeListener(listener) }
}

/**
 * Create a flow of attach and detach events on [View].
 */
@CheckResult
fun View.attachEvents(): Flow<ViewAttachEvent> = channelFlow {
    val listener = listener(this, ::offer)
    addOnAttachStateChangeListener(listener)
    awaitClose { removeOnAttachStateChangeListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (ViewAttachEvent) -> Boolean
) = object : View.OnAttachStateChangeListener {

    override fun onViewAttachedToWindow(v: View) { onEvent(ViewAttachAttachedEvent(v)) }
    override fun onViewDetachedFromWindow(v: View) { onEvent(ViewAttachDetachedEvent(v)) }

    private fun onEvent(event: ViewAttachEvent) {
        if (scope.isActive) { emitter(event) }
    }
}

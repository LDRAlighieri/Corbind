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

import android.os.Build
import android.view.View
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
import ru.ldralighieri.corbind.internal.safeOffer

data class ViewScrollChangeEvent(
    val view: View,
    val scrollX: Int,
    val scrollY: Int,
    val oldScrollX: Int,
    val oldScrollY: Int
)

/**
 * Perform an action on scroll-change events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.M)
fun View.scrollChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewScrollChangeEvent) -> Unit
) {

    val events = scope.actor<ViewScrollChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnScrollChangeListener(null) }
}

/**
 * Perform an action on scroll-change events for [View] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.M)
suspend fun View.scrollChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ViewScrollChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewScrollChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnScrollChangeListener(null) }
}

/**
 * Create a channel of scroll-change events for [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.M)
@CheckResult
fun View.scrollChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewScrollChangeEvent> = corbindReceiveChannel(capacity) {
    setOnScrollChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnScrollChangeListener(null) }
}

/**
 * Create a flow of scroll-change events for [View].
 */
@RequiresApi(Build.VERSION_CODES.M)
@CheckResult
fun View.scrollChangeEvents(): Flow<ViewScrollChangeEvent> = channelFlow {
    setOnScrollChangeListener(listener(this, ::offer))
    awaitClose { setOnScrollChangeListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (ViewScrollChangeEvent) -> Boolean
) = View.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
    if (scope.isActive) {
        emitter(ViewScrollChangeEvent(v, scrollX, scrollY, oldScrollX, oldScrollY))
    }
}

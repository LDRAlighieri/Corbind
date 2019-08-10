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
import android.view.ViewTreeObserver
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
import ru.ldralighieri.corbind.internal.offerElement

/**
 * Perform an action on draws on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun View.draws(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(scope, events::offer)
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

/**
 * Perform an action on draws on [View] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
suspend fun View.draws(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, capacity) {
        for (unit in channel) action()
    }

    val listener = listener(this, events::offer)
    viewTreeObserver.addOnDrawListener(listener)
    events.invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

/**
 * Create a channel for draws on [View].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
@CheckResult
fun View.draws(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    val listener = listener(scope, ::offerElement)
    viewTreeObserver.addOnDrawListener(listener)
    invokeOnClose { viewTreeObserver.removeOnDrawListener(listener) }
}

/**
 * Create a flow for draws on [View].
 */
@CheckResult
fun View.draws(): Flow<Unit> = channelFlow {
    val listener = listener(this, ::offer)
    viewTreeObserver.addOnDrawListener(listener)
    awaitClose { viewTreeObserver.removeOnDrawListener(listener) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (Unit) -> Boolean
) = ViewTreeObserver.OnDrawListener {
    if (scope.isActive) { emitter(Unit) }
}

/*
 * Copyright 2026 Vladimir Raupov
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
import androidx.annotation.MainThread
import com.google.android.material.listitem.ListItemCardView
import com.google.android.material.listitem.RevealableListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.checkMainThread
import ru.ldralighieri.corbind.internal.corbindCallbackFlow
import ru.ldralighieri.corbind.internal.corbindEventEmitter
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.invokeOnCloseOnMain

/**
 * Perform an action when the swipe state of this [ListItemCardView] changes. No initial state is
 * emitted; the containing `ListItemLayout` exposes its current state through `getSwipeState()`.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun ListItemCardView.swipeStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (state in channel) action(state)
    }

    val callback = swipeStateCallback(scope, events.corbindEventEmitter(scope))
    addSwipeCallback(callback)
    events.invokeOnCloseOnMain { removeSwipeCallback(callback) }
}

/** Perform an action on swipe state changes in a new [CoroutineScope]. */
@MainThread
suspend fun ListItemCardView.swipeStateChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    swipeStateChanges(this, capacity, action)
}

/**
 * Create a channel of swipe state changes. No initial state is emitted.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun ListItemCardView.swipeStateChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    val callback = swipeStateCallback(scope, corbindEventEmitter())
    addSwipeCallback(callback)
    awaitClose { removeSwipeCallback(callback) }
}

/** Create a flow of swipe state changes. Each collection adds and removes its own callback. */
@CheckResult
fun ListItemCardView.swipeStateChanges(): Flow<Int> = corbindCallbackFlow {
    val callback = swipeStateCallback(this, corbindEventEmitter())
    addSwipeCallback(callback)
    awaitClose { removeSwipeCallback(callback) }
}

private fun swipeStateCallback(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = object : ListItemCardView.SwipeCallback() {

    override fun onSwipe(swipeOffset: Int) = Unit

    override fun <T> onSwipeStateChanged(
        newState: Int,
        activeRevealableListItem: T,
        revealGravity: Int,
    ) where T : View, T : RevealableListItem {
        if (scope.isActive) emitter(newState)
    }
}

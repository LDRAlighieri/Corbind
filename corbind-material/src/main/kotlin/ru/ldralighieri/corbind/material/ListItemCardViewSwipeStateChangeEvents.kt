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

/** A swipe state change, including the revealed view and its gravity. */
data class ListItemSwipeStateChangeEvent(
    val cardView: ListItemCardView,
    val newState: Int,
    val revealableView: View,
    val revealGravity: Int,
)

/**
 * Perform an action on swipe state change events from this [ListItemCardView]. The
 * [ListItemSwipeStateChangeEvent.revealableView] also implements [RevealableListItem].
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun ListItemCardView.swipeStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ListItemSwipeStateChangeEvent) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<ListItemSwipeStateChangeEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    val callback = swipeStateEventCallback(this, scope, events.corbindEventEmitter(scope))
    addSwipeCallback(callback)
    events.invokeOnCloseOnMain { removeSwipeCallback(callback) }
}

/** Perform an action on swipe state change events in a new [CoroutineScope]. */
@MainThread
suspend fun ListItemCardView.swipeStateChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (ListItemSwipeStateChangeEvent) -> Unit,
) = coroutineScope {
    swipeStateChangeEvents(this, capacity, action)
}

/**
 * Create a channel of swipe state change events. No initial event is emitted.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun ListItemCardView.swipeStateChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<ListItemSwipeStateChangeEvent> = corbindReceiveChannel(scope, capacity) {
    val callback = swipeStateEventCallback(this@swipeStateChangeEvents, scope, corbindEventEmitter())
    addSwipeCallback(callback)
    awaitClose { removeSwipeCallback(callback) }
}

/** Create a flow of swipe state change events. Each collection adds and removes its own callback. */
@CheckResult
fun ListItemCardView.swipeStateChangeEvents(): Flow<ListItemSwipeStateChangeEvent> = corbindCallbackFlow {
    val callback = swipeStateEventCallback(this@swipeStateChangeEvents, this, corbindEventEmitter())
    addSwipeCallback(callback)
    awaitClose { removeSwipeCallback(callback) }
}

private fun swipeStateEventCallback(
    cardView: ListItemCardView,
    scope: CoroutineScope,
    emitter: (ListItemSwipeStateChangeEvent) -> Boolean,
) = object : ListItemCardView.SwipeCallback() {

    override fun onSwipe(swipeOffset: Int) = Unit

    override fun <T> onSwipeStateChanged(
        newState: Int,
        activeRevealableListItem: T,
        revealGravity: Int,
    ) where T : View, T : RevealableListItem {
        if (scope.isActive) {
            emitter(ListItemSwipeStateChangeEvent(cardView, newState, activeRevealableListItem, revealGravity))
        }
    }
}

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
 * Perform an action when the swipe offset of this [ListItemCardView] changes. Offsets are pixels
 * from the card's original position. Use [Channel.CONFLATED] if intermediate offsets may be dropped.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 * @param action An action to perform
 */
@MainThread
fun ListItemCardView.swipeOffsets(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) {
    checkMainThread()
    if (!scope.isActive) return

    val events = scope.actor<Int>(Dispatchers.Main.immediate, capacity) {
        for (offset in channel) action(offset)
    }

    val callback = swipeOffsetCallback(scope, events.corbindEventEmitter(scope))
    addSwipeCallback(callback)
    events.invokeOnCloseOnMain { removeSwipeCallback(callback) }
}

/** Perform an action on swipe offsets in a new [CoroutineScope]. */
@MainThread
suspend fun ListItemCardView.swipeOffsets(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (Int) -> Unit,
) = coroutineScope {
    swipeOffsets(this, capacity, action)
}

/**
 * Create a channel of swipe offsets in pixels. Use [Channel.CONFLATED] if intermediate offsets may
 * be dropped.
 *
 * @param scope Coroutine scope that owns the binding
 * @param capacity Capacity of the channel's buffer (no buffer by default). With suspending overflow,
 * events wait for delivery without blocking the Android callback thread.
 */
@CheckResult
fun ListItemCardView.swipeOffsets(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
): ReceiveChannel<Int> = corbindReceiveChannel(scope, capacity) {
    val callback = swipeOffsetCallback(scope, corbindEventEmitter())
    addSwipeCallback(callback)
    awaitClose { removeSwipeCallback(callback) }
}

/**
 * Create a flow of swipe offsets in pixels. Use `conflate()` if intermediate offsets may be
 * dropped. Each collection adds a callback and removes it when collection ends.
 */
@CheckResult
fun ListItemCardView.swipeOffsets(): Flow<Int> = corbindCallbackFlow {
    val callback = swipeOffsetCallback(this, corbindEventEmitter())
    addSwipeCallback(callback)
    awaitClose { removeSwipeCallback(callback) }
}

private fun swipeOffsetCallback(
    scope: CoroutineScope,
    emitter: (Int) -> Boolean,
) = object : ListItemCardView.SwipeCallback() {

    override fun onSwipe(swipeOffset: Int) {
        if (scope.isActive) emitter(swipeOffset)
    }

    override fun <T> onSwipeStateChanged(
        newState: Int,
        activeRevealableListItem: T,
        revealGravity: Int,
    ) where T : View, T : RevealableListItem = Unit
}

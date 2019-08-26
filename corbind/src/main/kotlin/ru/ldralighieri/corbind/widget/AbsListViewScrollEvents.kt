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

import android.widget.AbsListView
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

data class AbsListViewScrollEvent(
    val view: AbsListView,
    val scrollState: Int,
    val firstVisibleItem: Int,
    val visibleItemCount: Int,
    val totalItemCount: Int
)

/**
 * Perform an action on scroll events on [AbsListView].
 *
 * *Warning:* The created actor uses [AbsListView.setOnScrollListener] to emit scroll changes.
 * Only one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun AbsListView.scrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AbsListViewScrollEvent) -> Unit
) {

    val events = scope.actor<AbsListViewScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollListener(listener(scope, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}

/**
 * Perform an action on scroll events on [AbsListView] inside new CoroutineScope.
 *
 * *Warning:* The created actor uses [AbsListView.setOnScrollListener] to emit scroll changes.
 * Only one actor can be used for a view at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun AbsListView.scrollEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (AbsListViewScrollEvent) -> Unit
) = coroutineScope {

    val events = actor<AbsListViewScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollListener(listener(this, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}

/**
 * Create a channel of scroll events on [AbsListView].
 *
 * *Warning:* The created channel uses [AbsListView.setOnScrollListener] to emit scroll changes.
 * Only one channel can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun AbsListView.scrollEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AbsListViewScrollEvent> = corbindReceiveChannel(capacity) {
    setOnScrollListener(listener(scope, ::offerElement))
    invokeOnClose { setOnScrollListener(null) }
}

/**
 * Create a flow of scroll events on [AbsListView].
 *
 * *Warning:* The created flow uses [AbsListView.setOnScrollListener] to emit scroll changes.
 * Only one flow can be used for a view at a time.
 */
@CheckResult
fun AbsListView.scrollEvents(): Flow<AbsListViewScrollEvent> = channelFlow {
    setOnScrollListener(listener(this, ::offer))
    awaitClose { setOnScrollListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (AbsListViewScrollEvent) -> Boolean
) = object : AbsListView.OnScrollListener {

    private var currentScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE

    override fun onScrollStateChanged(absListView: AbsListView, scrollState: Int) {
        currentScrollState = scrollState
        if (scope.isActive) {
            val event = AbsListViewScrollEvent(absListView, scrollState,
                    absListView.firstVisiblePosition, absListView.childCount, absListView.count)
            emitter(event)
        }
    }

    override fun onScroll(
        absListView: AbsListView,
        firstVisibleItem: Int,
        visibleItemCount: Int,
        totalItemCount: Int
    ) {
        if (scope.isActive) {
            val event = AbsListViewScrollEvent(absListView, currentScrollState, firstVisibleItem,
                    visibleItemCount, totalItemCount)
            emitter(event)
        }
    }
}

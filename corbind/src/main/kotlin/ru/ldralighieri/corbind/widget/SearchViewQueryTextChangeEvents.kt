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

import android.widget.SearchView
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

data class SearchViewQueryTextEvent(
    val view: SearchView,
    val queryText: CharSequence,
    val isSubmitted: Boolean
)

/**
 * Perform an action on [query text events][SearchViewQueryTextEvent] on [SearchView]
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchView.queryTextChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewQueryTextEvent) -> Unit
) {

    val events = scope.actor<SearchViewQueryTextEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    setOnQueryTextListener(listener(scope = scope, searchView = this, emitter = events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

/**
 * Perform an action on  [query text events][SearchViewQueryTextEvent] on [SearchView] inside new
 * CoroutineScope
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchView.queryTextChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchViewQueryTextEvent) -> Unit
) = coroutineScope {

    val events = actor<SearchViewQueryTextEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@queryTextChangeEvents))
    setOnQueryTextListener(listener(scope = this, searchView = this@queryTextChangeEvents,
            emitter = events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

/**
 * Create a channel of [query text events][SearchViewQueryTextEvent] on [SearchView]
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchView.queryTextChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SearchViewQueryTextEvent> = corbindReceiveChannel(capacity) {
    offerElement(initialValue(this@queryTextChangeEvents))
    setOnQueryTextListener(listener(scope, this@queryTextChangeEvents, ::offerElement))
    invokeOnClose { setOnQueryTextListener(null) }
}

/**
 * Create a flow of [query text events][SearchViewQueryTextEvent] on [SearchView]
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun SearchView.queryTextChangeEvents(): Flow<SearchViewQueryTextEvent> = channelFlow {
    offer(initialValue(this@queryTextChangeEvents))
    setOnQueryTextListener(listener(this, this@queryTextChangeEvents, ::offer))
    awaitClose { setOnQueryTextListener(null) }
}

@CheckResult
private fun initialValue(searchView: SearchView): SearchViewQueryTextEvent =
        SearchViewQueryTextEvent(searchView, searchView.query, false)

@CheckResult
private fun listener(
    scope: CoroutineScope,
    searchView: SearchView,
    emitter: (SearchViewQueryTextEvent) -> Boolean
) = object : SearchView.OnQueryTextListener {

    override fun onQueryTextChange(s: String): Boolean {
        return onEvent(SearchViewQueryTextEvent(searchView, s, false))
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return onEvent(SearchViewQueryTextEvent(searchView, query, true))
    }

    private fun onEvent(event: SearchViewQueryTextEvent): Boolean {
        if (scope.isActive) {
            emitter(event)
            return true
        }
        return false
    }
}

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

package ru.ldralighieri.corbind.leanback

import androidx.annotation.CheckResult
import androidx.leanback.widget.SearchBar
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

sealed class SearchBarSearchQueryEvent {
    abstract val view: SearchBar
    abstract val searchQuery: String
}

data class SearchBarSearchQueryChangedEvent(
    override val view: SearchBar,
    override val searchQuery: String
) : SearchBarSearchQueryEvent()

data class SearchBarSearchQueryKeyboardDismissedEvent(
    override val view: SearchBar,
    override val searchQuery: String
) : SearchBarSearchQueryEvent()

data class SearchBarSearchQuerySubmittedEvent(
    override val view: SearchBar,
    override val searchQuery: String
) : SearchBarSearchQueryEvent()

/**
 * Perform an action on [search query events][SearchBarSearchQueryEvent] on [SearchBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchBar.searchQueryChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchBarSearchQueryEvent) -> Unit
) {

    val events = scope.actor<SearchBarSearchQueryEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setSearchBarListener(listener(scope = scope, searchBar = this, emitter = events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

/**
 * Perform an action on [search query events][SearchBarSearchQueryEvent] on [SearchBar] inside new
 * [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchBar.searchQueryChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchBarSearchQueryEvent) -> Unit
) = coroutineScope {

    val events = actor<SearchBarSearchQueryEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setSearchBarListener(listener(scope = this, searchBar = this@searchQueryChangeEvents,
            emitter = events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

/**
 * Create a channel of [search query events][SearchBarSearchQueryEvent] on [SearchBar].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchBar.searchQueryChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SearchBarSearchQueryEvent> = corbindReceiveChannel(capacity) {
    setSearchBarListener(listener(scope, this@searchQueryChangeEvents, ::offerElement))
    invokeOnClose { setSearchBarListener(null) }
}

/**
 * Create a flow of [search query events][SearchBarSearchQueryEvent] on [SearchBar].
 */
@CheckResult
fun SearchBar.searchQueryChangeEvents(): Flow<SearchBarSearchQueryEvent> = channelFlow {
    setSearchBarListener(listener(this, this@searchQueryChangeEvents, ::offer))
    awaitClose { setSearchBarListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    searchBar: SearchBar,
    emitter: (SearchBarSearchQueryEvent) -> Boolean
) = object : SearchBar.SearchBarListener {

    override fun onSearchQueryChange(query: String) {
        onEvent(SearchBarSearchQueryChangedEvent(searchBar, query))
    }

    override fun onSearchQuerySubmit(query: String) {
        onEvent(SearchBarSearchQuerySubmittedEvent(searchBar, query))
    }

    override fun onKeyboardDismiss(query: String) {
        onEvent(SearchBarSearchQueryKeyboardDismissedEvent(searchBar, query))
    }

    private fun onEvent(evennt: SearchBarSearchQueryEvent) {
        if (scope.isActive) { emitter(evennt) }
    }
}

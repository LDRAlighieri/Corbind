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
import ru.ldralighieri.corbind.internal.offerCatching

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
 * *Warning:* The created actor uses [SearchBar.setSearchBarListener]. Only one actor can be used at
 * a time.
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
    val events = scope.actor<SearchBarSearchQueryEvent>(Dispatchers.Main.immediate, capacity) {
        for (event in channel) action(event)
    }

    setSearchBarListener(listener(scope, this, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

/**
 * Perform an action on [search query events][SearchBarSearchQueryEvent] on [SearchBar], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SearchBar.setSearchBarListener]. Only one actor can be used at
 * a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchBar.searchQueryChangeEvents(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (SearchBarSearchQueryEvent) -> Unit
) = coroutineScope {
    searchQueryChangeEvents(this, capacity, action)
}

/**
 * Create a channel of [search query events][SearchBarSearchQueryEvent] on [SearchBar].
 *
 * *Warning:* The created channel uses [SearchBar.setSearchBarListener]. Only one channel can be
 * used at a time.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * launch {
 *      searchBar.searchQueryChangeEvents(scope)
 *          .consumeEach { event ->
 *              when (event) {
 *                  is SearchBarSearchQueryChangedEvent -> { /* handle query change event */ }
 *                  is SearchBarSearchQueryKeyboardDismissedEvent -> {
 *                      /* handle Keyboard dismiss event */
 *                  }
 *                  is SearchBarSearchQuerySubmittedEvent -> { /* handle query submit event */ }
 *              }
 *          }
 * }
 *
 * // handle one event
 * launch {
 *      searchBar.searchQueryChangeEvents(scope)
 *          .filterIsInstance<SearchBarSearchQueryChangedEvent>()
 *          .consumeEach { /* handle query change event */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchBar.searchQueryChangeEvents(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SearchBarSearchQueryEvent> = corbindReceiveChannel(capacity) {
    setSearchBarListener(listener(scope, this@searchQueryChangeEvents, ::offerCatching))
    invokeOnClose { setSearchBarListener(null) }
}

/**
 * Create a flow of [search query events][SearchBarSearchQueryEvent] on [SearchBar].
 *
 * *Warning:* The created flow uses [SearchBar.setSearchBarListener]. Only one flow can be used at a
 * time.
 *
 * Examples:
 *
 * ```
 * // handle all events
 * searchBar.searchQueryChangeEvents()
 *      .onEach { event ->
 *          when (event) {
 *              is SearchBarSearchQueryChangedEvent -> { /* handle query change event */ }
 *              is SearchBarSearchQueryKeyboardDismissedEvent -> {
 *                  /* handle Keyboard dismiss event */
 *              }
 *              is SearchBarSearchQuerySubmittedEvent -> { /* handle query submit event */ }
 *          }
 *      }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 *
 * // handle one event
 * searchBar.searchQueryChangeEvents()
 *      .filterIsInstance<SearchBarSearchQueryChangedEvent>()
 *      .onEach { /* handle query change event */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 */
@CheckResult
fun SearchBar.searchQueryChangeEvents(): Flow<SearchBarSearchQueryEvent> = channelFlow {
    setSearchBarListener(listener(this, this@searchQueryChangeEvents, ::offerCatching))
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

    private fun onEvent(event: SearchBarSearchQueryEvent) {
        if (scope.isActive) { emitter(event) }
    }
}

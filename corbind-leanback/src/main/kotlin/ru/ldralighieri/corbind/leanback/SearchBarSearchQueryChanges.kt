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

/**
 * Perform an action on String values for search query changes on [SearchBar].
 *
 * *Warning:* The created actor uses [SearchBar.setSearchBarListener]. Only one actor can be used at
 * a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun SearchBar.searchQueryChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (String) -> Unit
) {
    val events = scope.actor<String>(Dispatchers.Main.immediate, capacity) {
        for (query in channel) action(query)
    }

    setSearchBarListener(listener(scope, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

/**
 * Perform an action on String values for search query changes on [SearchBar], inside new
 * [CoroutineScope].
 *
 * *Warning:* The created actor uses [SearchBar.setSearchBarListener]. Only one actor can be used at
 * a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun SearchBar.searchQueryChanges(
    capacity: Int = Channel.RENDEZVOUS,
    action: suspend (String) -> Unit
) = coroutineScope {
    searchQueryChanges(this, capacity, action)
}

/**
 * Create a channel of String values for search query changes on [SearchBar].
 *
 * *Warning:* The created channel uses [SearchBar.setSearchBarListener]. Only one channel can be
 * used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      absListView.scrollEvents(scope)
 *          .consumeEach { /* handle query change */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun SearchBar.searchQueryChanges(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<String> = corbindReceiveChannel(capacity) {
    setSearchBarListener(listener(scope, ::offerCatching))
    invokeOnClose { setSearchBarListener(null) }
}

/**
 * Create a flow of String values for search query changes on [SearchBar].
 *
 * *Warning:* The created flow uses [SearchBar.setSearchBarListener]. Only one flow can be used at a
 * time.
 *
 * Example:
 *
 * ```
 * searchBar.searchQueryChanges()
 *      .onEach { /* handle query change */ }
 *      .launchIn(lifecycleScope) // lifecycle-runtime-ktx
 * ```
 */
@CheckResult
fun SearchBar.searchQueryChanges(): Flow<String> = channelFlow<String> {
    setSearchBarListener(listener(this, ::offerCatching))
    awaitClose { setSearchBarListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    emitter: (String) -> Boolean
) = object : SearchBar.SearchBarListener {

    override fun onSearchQueryChange(query: String) {
        if (scope.isActive) { emitter(query) }
    }

    override fun onSearchQuerySubmit(query: String) = Unit
    override fun onKeyboardDismiss(query: String) = Unit
}

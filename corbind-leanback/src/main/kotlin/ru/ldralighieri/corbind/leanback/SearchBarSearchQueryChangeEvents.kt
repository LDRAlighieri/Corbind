package ru.ldralighieri.corbind.leanback

import androidx.leanback.widget.SearchBar
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

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

// -----------------------------------------------------------------------------------------------


fun SearchBar.searchQueryChangeEvents(
        scope: CoroutineScope,
        action: suspend (SearchBarSearchQueryEvent) -> Unit
) {
    val events = scope.actor<SearchBarSearchQueryEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setSearchBarListener(listener(this, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

suspend fun SearchBar.searchQueryChangeEvents(
        action: suspend (SearchBarSearchQueryEvent) -> Unit
) = coroutineScope {
    val events = actor<SearchBarSearchQueryEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setSearchBarListener(listener(this@searchQueryChangeEvents, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SearchBar.searchQueryChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<SearchBarSearchQueryEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setSearchBarListener(listener(this@searchQueryChangeEvents, ::offer))
    invokeOnClose { setSearchBarListener(null) }
}

suspend fun SearchBar.searchQueryChangeEvents(): ReceiveChannel<SearchBarSearchQueryEvent> = coroutineScope {

    produce<SearchBarSearchQueryEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setSearchBarListener(listener(this@searchQueryChangeEvents, ::offer))
        invokeOnClose { setSearchBarListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        searchBar: SearchBar,
        emitter: (SearchBarSearchQueryEvent) -> Boolean
) = object : SearchBar.SearchBarListener {

    override fun onSearchQueryChange(query: String) {
        emitter(SearchBarSearchQueryChangedEvent(searchBar, query))
    }

    override fun onSearchQuerySubmit(query: String) {
        emitter(SearchBarSearchQuerySubmittedEvent(searchBar, query))
    }

    override fun onKeyboardDismiss(query: String) {
        emitter(SearchBarSearchQueryKeyboardDismissedEvent(searchBar, query))
    }
}
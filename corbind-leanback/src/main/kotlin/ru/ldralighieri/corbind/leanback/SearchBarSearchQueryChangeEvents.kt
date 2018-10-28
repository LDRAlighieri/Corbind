package ru.ldralighieri.corbind.leanback

import androidx.annotation.CheckResult
import androidx.leanback.widget.SearchBar
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

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

    setSearchBarListener(listener(scope = scope, searchBar = this, emitter = events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

suspend fun SearchBar.searchQueryChangeEvents(
        action: suspend (SearchBarSearchQueryEvent) -> Unit
) = coroutineScope {

    val events = actor<SearchBarSearchQueryEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setSearchBarListener(listener(scope = this, searchBar = this@searchQueryChangeEvents,
            emitter = events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchBar.searchQueryChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<SearchBarSearchQueryEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setSearchBarListener(listener(scope = this, searchBar = this@searchQueryChangeEvents,
            emitter = ::offer))
    invokeOnClose { setSearchBarListener(null) }
}

@CheckResult
suspend fun SearchBar.searchQueryChangeEvents(): ReceiveChannel<SearchBarSearchQueryEvent> = coroutineScope {

    produce<SearchBarSearchQueryEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setSearchBarListener(listener(scope = this, searchBar = this@searchQueryChangeEvents,
                emitter = ::offer))
        invokeOnClose { setSearchBarListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


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
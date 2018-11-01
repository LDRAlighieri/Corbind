@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.leanback

import androidx.annotation.CheckResult
import androidx.leanback.widget.SearchBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

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
): ReceiveChannel<SearchBarSearchQueryEvent> = corbindReceiveChannel {

    setSearchBarListener(listener(scope, this@searchQueryChangeEvents, ::safeOffer))
    invokeOnClose { setSearchBarListener(null) }
}

@CheckResult
suspend fun SearchBar.searchQueryChangeEvents(): ReceiveChannel<SearchBarSearchQueryEvent> = coroutineScope {

    corbindReceiveChannel<SearchBarSearchQueryEvent> {
        setSearchBarListener(listener(this@coroutineScope, this@searchQueryChangeEvents,
                ::safeOffer))
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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.appcompat

import androidx.annotation.CheckResult
import androidx.appcompat.widget.SearchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------

data class SearchViewQueryTextEvent(
        val view: SearchView,
        val queryText: CharSequence,
        val isSubmitted: Boolean
)

// -----------------------------------------------------------------------------------------------


fun SearchView.queryTextChangeEvents(
        scope: CoroutineScope,
        action: suspend (SearchViewQueryTextEvent) -> Unit
) {

    val events = scope.actor<SearchViewQueryTextEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(SearchViewQueryTextEvent(this, query, false))
    setOnQueryTextListener(listener(scope = scope, searchView = this, emitter = events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

suspend fun SearchView.queryTextChangeEvents(
        action: suspend (SearchViewQueryTextEvent) -> Unit
) = coroutineScope {

    val events = actor<SearchViewQueryTextEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(SearchViewQueryTextEvent(this@queryTextChangeEvents, query, false))
    setOnQueryTextListener(listener(scope = this, searchView = this@queryTextChangeEvents,
            emitter = events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchView.queryTextChangeEvents(
        scope: CoroutineScope
): ReceiveChannel<SearchViewQueryTextEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(SearchViewQueryTextEvent(this@queryTextChangeEvents, query, false))
    setOnQueryTextListener(listener(scope = this, searchView = this@queryTextChangeEvents,
            emitter = ::offer))
    invokeOnClose { setOnQueryTextListener(null) }
}

@CheckResult
suspend fun SearchView.queryTextChangeEvents(): ReceiveChannel<SearchViewQueryTextEvent> = coroutineScope {

    produce<SearchViewQueryTextEvent>(Dispatchers.Main, Channel.CONFLATED) {
        offer(SearchViewQueryTextEvent(this@queryTextChangeEvents, query, false))
        setOnQueryTextListener(listener(scope = this, searchView = this@queryTextChangeEvents,
                emitter = ::offer))
        invokeOnClose { setOnQueryTextListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


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
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.SearchView
import androidx.annotation.CheckResult
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

data class SearchViewQueryTextEvent(
        val view: SearchView,
        val queryText: CharSequence,
        val isSubmitted: Boolean
)

// -----------------------------------------------------------------------------------------------


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


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchView.queryTextChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<SearchViewQueryTextEvent> = corbindReceiveChannel(capacity) {

    safeOffer(initialValue(this@queryTextChangeEvents))
    setOnQueryTextListener(listener(scope, this@queryTextChangeEvents, ::safeOffer))
    invokeOnClose { setOnQueryTextListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(searchView: SearchView): SearchViewQueryTextEvent =
        SearchViewQueryTextEvent(searchView, searchView.query, false)


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
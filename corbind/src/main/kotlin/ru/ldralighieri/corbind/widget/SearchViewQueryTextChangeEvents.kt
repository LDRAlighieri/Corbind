package ru.ldralighieri.corbind.widget

import android.widget.SearchView
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

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

    events.offer(initialValue(this))
    setOnQueryTextListener(listener(scope = scope, searchView = this, emitter = events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

suspend fun SearchView.queryTextChangeEvents(
        action: suspend (SearchViewQueryTextEvent) -> Unit
) = coroutineScope {

    val events = actor<SearchViewQueryTextEvent>(Dispatchers.Main, Channel.CONFLATED) {
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
        scope: CoroutineScope
): ReceiveChannel<SearchViewQueryTextEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@queryTextChangeEvents))
    setOnQueryTextListener(listener(scope = this, searchView = this@queryTextChangeEvents,
            emitter = ::offer))
    invokeOnClose { setOnQueryTextListener(null) }
}

@CheckResult
suspend fun SearchView.queryTextChangeEvents(): ReceiveChannel<SearchViewQueryTextEvent> = coroutineScope {

    produce<SearchViewQueryTextEvent>(Dispatchers.Main, Channel.CONFLATED) {
        offer(initialValue(this@queryTextChangeEvents))
        setOnQueryTextListener(listener(scope = this, searchView = this@queryTextChangeEvents,
                emitter = ::offer))
        invokeOnClose { setOnQueryTextListener(null) }
    }
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
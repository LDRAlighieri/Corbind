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


fun SearchBar.searchQueryChanges(
        scope: CoroutineScope,
        action: suspend (String) -> Unit
) {

    val events = scope.actor<String>(Dispatchers.Main, Channel.CONFLATED) {
        for (query in channel) action(query)
    }

    setSearchBarListener(listener(scope, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}

suspend fun SearchBar.searchQueryChanges(
        action: suspend (String) -> Unit
) = coroutineScope {

    val events = actor<String>(Dispatchers.Main, Channel.CONFLATED) {
        for (query in channel) action(query)
    }

    setSearchBarListener(listener(this, events::offer))
    events.invokeOnClose { setSearchBarListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchBar.searchQueryChanges(
        scope: CoroutineScope
): ReceiveChannel<String> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setSearchBarListener(listener(this, ::offer))
    invokeOnClose { setSearchBarListener(null) }
}

@CheckResult
suspend fun SearchBar.searchQueryChanges(): ReceiveChannel<String> = coroutineScope {

    produce<String>(Dispatchers.Main, Channel.CONFLATED) {
        setSearchBarListener(listener(this, ::offer))
        invokeOnClose { setSearchBarListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (String) -> Boolean
) = object : SearchBar.SearchBarListener {

    override fun onSearchQueryChange(query: String) {
        if (scope.isActive) { emitter(query) }
    }

    override fun onSearchQuerySubmit(query: String) {  }
    override fun onKeyboardDismiss(query: String) {  }
}
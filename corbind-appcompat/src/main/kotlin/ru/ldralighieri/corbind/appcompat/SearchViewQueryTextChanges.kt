@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.appcompat

import androidx.annotation.CheckResult
import androidx.appcompat.widget.SearchView
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


fun SearchView.queryTextChanges(
        scope: CoroutineScope,
        action: suspend (CharSequence) -> Unit
) {

    val events = scope.actor<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        for (chars in channel) action(chars)
    }

    events.offer(query)
    setOnQueryTextListener(listener(scope, events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}

suspend fun SearchView.queryTextChanges(
        action: suspend (CharSequence) -> Unit
) = coroutineScope {

    val events = actor<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        for (chars in channel) action(chars)
    }

    events.offer(query)
    setOnQueryTextListener(listener(this, events::offer))
    events.invokeOnClose { setOnQueryTextListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchView.queryTextChanges(
        scope: CoroutineScope
): ReceiveChannel<CharSequence> = corbindReceiveChannel {

    safeOffer(query)
    setOnQueryTextListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnQueryTextListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (CharSequence) -> Boolean
) = object : SearchView.OnQueryTextListener {

    override fun onQueryTextChange(s: String): Boolean {
        if (scope.isActive) {
            emitter(s)
            return true
        }
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean { return false }
}
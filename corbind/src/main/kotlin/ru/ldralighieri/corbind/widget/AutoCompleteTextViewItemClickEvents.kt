@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
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


fun AutoCompleteTextView.itemClickEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (AdapterViewItemClickEvent) -> Unit
) {

    val events = scope.actor<AdapterViewItemClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(scope, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

suspend fun AutoCompleteTextView.itemClickEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (AdapterViewItemClickEvent) -> Unit
) = coroutineScope {

    val events = actor<AdapterViewItemClickEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(this, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun AutoCompleteTextView.itemClickEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AdapterViewItemClickEvent> = corbindReceiveChannel(capacity) {

    onItemClickListener = listener(scope, ::safeOffer)
    invokeOnClose { onItemClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (AdapterViewItemClickEvent) -> Boolean
) = AdapterView.OnItemClickListener { parent, view: View?, position, id ->

    if (scope.isActive) {
        emitter(AdapterViewItemClickEvent(parent, view, position, id))
    }
}
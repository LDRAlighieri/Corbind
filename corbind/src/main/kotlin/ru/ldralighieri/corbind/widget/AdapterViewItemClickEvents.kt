package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

data class AdapterViewItemClickEvent(
        val view: AdapterView<*>,
        val clickedView: View,
        val position: Int,
        val id: Long
)

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemClickEvents(
        scope: CoroutineScope,
        action: suspend (AdapterViewItemClickEvent) -> Unit
) {
    val events = scope.actor<AdapterViewItemClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemClickEvents(
        action: suspend (AdapterViewItemClickEvent) -> Unit
) = coroutineScope {
    val events = actor<AdapterViewItemClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onItemClickListener = listener(events::offer)
    events.invokeOnClose { onItemClickListener = null }
}


// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemClickEvents(
        scope: CoroutineScope
): ReceiveChannel<AdapterViewItemClickEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    onItemClickListener = listener(::offer)
    invokeOnClose { onItemClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemClickEvents()
        : ReceiveChannel<AdapterViewItemClickEvent> = coroutineScope {

    produce<AdapterViewItemClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        onItemClickListener = listener(::offer)
        invokeOnClose { onItemClickListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (AdapterViewItemClickEvent) -> Boolean
) = AdapterView.OnItemClickListener { parent, view, position, id ->
    emitter(AdapterViewItemClickEvent(parent, view, position, id))
}
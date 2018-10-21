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

sealed class AdapterViewSelectionEvent {
    abstract val view: AdapterView<*>
}

data class AdapterViewItemSelectionEvent(
        override val view: AdapterView<*>,
        val selectedView: View,
        val position: Int,
        val id: Long
) : AdapterViewSelectionEvent()

data class AdapterViewNothingSelectionEvent(
        override val view: AdapterView<*>
) : AdapterViewSelectionEvent()

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.selectionEvents(
        scope: CoroutineScope,
        action: suspend (AdapterViewSelectionEvent) -> Unit
) {
    val events = scope.actor<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    onItemSelectedListener = listener(events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.selectionEvents(
        action: suspend (AdapterViewSelectionEvent) -> Unit
) = coroutineScope {
    val events = actor<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.selectionEvents(
        scope: CoroutineScope
): ReceiveChannel<AdapterViewSelectionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(::offer)
    invokeOnClose { onItemSelectedListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.selectionEvents()
        : ReceiveChannel<AdapterViewSelectionEvent> = coroutineScope {

    produce<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        offer(initialValue(this@selectionEvents))
        onItemSelectedListener = listener(::offer)
        invokeOnClose { onItemSelectedListener = null }
    }
}


// -----------------------------------------------------------------------------------------------

private fun <T : Adapter> initialValue(adapterView: AdapterView<T>): AdapterViewSelectionEvent {
    return if (adapterView.selectedItemPosition == AdapterView.INVALID_POSITION) {
        AdapterViewNothingSelectionEvent(adapterView)
    } else {
        AdapterViewItemSelectionEvent(adapterView, adapterView.selectedView,
                adapterView.selectedItemPosition, adapterView.selectedItemId)
    }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        emitter: (AdapterViewSelectionEvent) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        emitter(AdapterViewItemSelectionEvent(parent, view, position, id))
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        emitter(AdapterViewNothingSelectionEvent(parent))
    }
}
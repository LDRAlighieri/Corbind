package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
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
    onItemSelectedListener = listener(scope, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.selectionEvents(
        action: suspend (AdapterViewSelectionEvent) -> Unit
) = coroutineScope {

    val events = actor<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(this, events::offer)
    events.invokeOnClose { onItemSelectedListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.selectionEvents(
        scope: CoroutineScope
): ReceiveChannel<AdapterViewSelectionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(initialValue(this@selectionEvents))
    onItemSelectedListener = listener(this, ::offer)
    invokeOnClose { onItemSelectedListener = null }
}

@CheckResult
suspend fun <T : Adapter> AdapterView<T>.selectionEvents()
        : ReceiveChannel<AdapterViewSelectionEvent> = coroutineScope {

    produce<AdapterViewSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        offer(initialValue(this@selectionEvents))
        onItemSelectedListener = listener(this, ::offer)
        invokeOnClose { onItemSelectedListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun <T : Adapter> initialValue(adapterView: AdapterView<T>): AdapterViewSelectionEvent {
    return if (adapterView.selectedItemPosition == AdapterView.INVALID_POSITION) {
        AdapterViewNothingSelectionEvent(adapterView)
    } else {
        AdapterViewItemSelectionEvent(adapterView, adapterView.selectedView,
                adapterView.selectedItemPosition, adapterView.selectedItemId)
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (AdapterViewSelectionEvent) -> Boolean
) = object : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        onEvent(AdapterViewItemSelectionEvent(parent, view, position, id))
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        onEvent(AdapterViewNothingSelectionEvent(parent))
    }

    private fun onEvent(event: AdapterViewSelectionEvent) {
        if (scope.isActive) { emitter(event) }
    }
}
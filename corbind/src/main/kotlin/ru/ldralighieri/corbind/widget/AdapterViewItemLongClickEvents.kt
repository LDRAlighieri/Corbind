@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

data class AdapterViewItemLongClickEvent(
        val view: AdapterView<*>,
        val clickedView: View,
        val position: Int,
        val id: Long
)

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        scope: CoroutineScope,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
        action: suspend (AdapterViewItemLongClickEvent) -> Unit
) {

    val events = scope.actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(scope, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue,
        action: suspend (AdapterViewItemLongClickEvent) -> Unit
) = coroutineScope {

    val events = actor<AdapterViewItemLongClickEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    onItemLongClickListener = listener(this, handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClickEvents(
        scope: CoroutineScope,
        handled: (AdapterViewItemLongClickEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<AdapterViewItemLongClickEvent> = corbindReceiveChannel {

    onItemLongClickListener = listener(scope, handled, ::safeOffer)
    invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (AdapterViewItemLongClickEvent) -> Boolean,
        emitter: (AdapterViewItemLongClickEvent) -> Boolean
) = AdapterView.OnItemLongClickListener { parent, view, position, id ->

    if (scope.isActive) {
        val event = AdapterViewItemLongClickEvent(parent, view, position, id)
        if (handled(event)) {
            emitter(event)
            return@OnItemLongClickListener true
        }
    }
    return@OnItemLongClickListener false
}
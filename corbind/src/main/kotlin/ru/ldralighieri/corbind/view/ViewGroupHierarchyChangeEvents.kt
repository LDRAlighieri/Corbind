package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce

fun ViewGroup.changeEvents(
        scope: CoroutineScope,
        action: suspend (ViewGroupHierarchyChangeEvent) -> Unit
) {
    val events = scope.actor<ViewGroupHierarchyChangeEvent>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnHierarchyChangeListener(listener(viewGroup = this, emitter = events::offer))
    events.invokeOnClose { setOnHierarchyChangeListener(null) }
}

// -----------------------------------------------------------------------------------------------

fun ViewGroup.changeEvents(
        scope: CoroutineScope
): ReceiveChannel<ViewGroupHierarchyChangeEvent> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    setOnHierarchyChangeListener(listener(viewGroup = this@changeEvents, emitter = ::offer))
    invokeOnClose { setOnHierarchyChangeListener(null) }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        viewGroup: ViewGroup,
        emitter: (ViewGroupHierarchyChangeEvent) -> Boolean
) = object : ViewGroup.OnHierarchyChangeListener {
    override fun onChildViewAdded(parent: View, child: View) {
        emitter(ViewGroupHierarchyChildViewAddEvent(viewGroup, child))
    }

    override fun onChildViewRemoved(parent: View?, child: View) {
        emitter(ViewGroupHierarchyChildViewRemoveEvent(viewGroup, child))
    }
}
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.View
import android.view.ViewGroup
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

sealed class ViewGroupHierarchyChangeEvent {
    abstract val view: ViewGroup
    abstract val child: View
}

data class ViewGroupHierarchyChildViewAddEvent(
        override val view: ViewGroup,
        override val child: View
) : ViewGroupHierarchyChangeEvent()

data class ViewGroupHierarchyChildViewRemoveEvent(
        override val view: ViewGroup,
        override val child: View
) : ViewGroupHierarchyChangeEvent()

// -----------------------------------------------------------------------------------------------


fun ViewGroup.changeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewGroupHierarchyChangeEvent) -> Unit
) {

    val events = scope.actor<ViewGroupHierarchyChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnHierarchyChangeListener(listener(scope = scope, viewGroup = this, emitter = events::offer))
    events.invokeOnClose { setOnHierarchyChangeListener(null) }
}

suspend fun ViewGroup.changeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (ViewGroupHierarchyChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<ViewGroupHierarchyChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnHierarchyChangeListener(listener(scope = this, viewGroup = this@changeEvents,
            emitter = events::offer))
    events.invokeOnClose { setOnHierarchyChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun ViewGroup.changeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<ViewGroupHierarchyChangeEvent> = corbindReceiveChannel(capacity) {

    setOnHierarchyChangeListener(listener(scope, this@changeEvents, ::safeOffer))
    invokeOnClose { setOnHierarchyChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        viewGroup: ViewGroup,
        emitter: (ViewGroupHierarchyChangeEvent) -> Boolean
) = object : ViewGroup.OnHierarchyChangeListener {

    override fun onChildViewAdded(parent: View, child: View) {
        onEvent(ViewGroupHierarchyChildViewAddEvent(viewGroup, child))
    }

    override fun onChildViewRemoved(parent: View, child: View) {
        onEvent(ViewGroupHierarchyChildViewRemoveEvent(viewGroup, child))
    }

    private fun onEvent(event: ViewGroupHierarchyChangeEvent) {
        if (scope.isActive) { emitter(event) }
    }
}
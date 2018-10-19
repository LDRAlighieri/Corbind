package ru.ldralighieri.corbind.view

import android.view.MenuItem
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

fun MenuItem.actionViewEvents(
        scope: CoroutineScope,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
        action: suspend (MenuItemActionViewEvent) -> Unit
) {
    val events = scope.actor<MenuItemActionViewEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}

suspend fun MenuItem.actionViewEvents(
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
        action: suspend (MenuItemActionViewEvent) -> Unit
) = coroutineScope {
    val events = actor<MenuItemActionViewEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun MenuItem.actionViewEvents(
        scope: CoroutineScope,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItemActionViewEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnActionExpandListener(listener(handled, ::offer))
    invokeOnClose { setOnActionExpandListener(null) }
}

suspend fun MenuItem.actionViewEvents(
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItemActionViewEvent> = coroutineScope {

    produce<MenuItemActionViewEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnActionExpandListener(listener(handled, ::offer))
        invokeOnClose { setOnActionExpandListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: (MenuItemActionViewEvent) -> Boolean,
        emitter: (MenuItemActionViewEvent) -> Boolean
) = object : MenuItem.OnActionExpandListener {
    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewExpandEvent(item))
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewCollapseEvent(item))
    }

    fun onEvent(event: MenuItemActionViewEvent): Boolean {
        return if (handled.invoke(event)) { emitter(event) }
        else { false }
    }
}
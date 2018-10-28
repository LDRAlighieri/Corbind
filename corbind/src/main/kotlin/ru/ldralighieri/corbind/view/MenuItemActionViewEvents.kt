package ru.ldralighieri.corbind.view

import android.view.MenuItem
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

sealed class MenuItemActionViewEvent {
    abstract val menuItem: MenuItem
}

data class MenuItemActionViewCollapseEvent(
        override val menuItem: MenuItem
) : MenuItemActionViewEvent()

data class MenuItemActionViewExpandEvent(
        override val menuItem: MenuItem
) : MenuItemActionViewEvent()

// -----------------------------------------------------------------------------------------------


fun MenuItem.actionViewEvents(
        scope: CoroutineScope,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
        action: suspend (MenuItemActionViewEvent) -> Boolean
) {

    val events = scope.actor<MenuItemActionViewEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}

suspend fun MenuItem.actionViewEvents(
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue,
        action: suspend (MenuItemActionViewEvent) -> Unit
) = coroutineScope {

    val events = actor<MenuItemActionViewEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnActionExpandListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnActionExpandListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun MenuItem.actionViewEvents(
        scope: CoroutineScope,
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItemActionViewEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnActionExpandListener(listener(this, handled, ::offer))
    invokeOnClose { setOnActionExpandListener(null) }
}

@CheckResult
suspend fun MenuItem.actionViewEvents(
        handled: (MenuItemActionViewEvent) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItemActionViewEvent> = coroutineScope {

    produce<MenuItemActionViewEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnActionExpandListener(listener(this, handled, ::offer))
        invokeOnClose { setOnActionExpandListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MenuItemActionViewEvent) -> Boolean,
        emitter: (MenuItemActionViewEvent) -> Boolean
) = object : MenuItem.OnActionExpandListener {

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewExpandEvent(item))
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        return onEvent(MenuItemActionViewCollapseEvent(item))
    }

    private fun onEvent(event: MenuItemActionViewEvent): Boolean {
        if (scope.isActive) {
            if (handled(event)) {
                emitter(event)
                return true
            }
        }
        return false
    }
}
package ru.ldralighieri.corbind.view

import android.view.MenuItem
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

fun MenuItem.clicks(
        scope: CoroutineScope,
        handled: (MenuItem) -> Boolean = AlwaysTrue,
        action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(handled = handled, emitter = events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

// -----------------------------------------------------------------------------------------------

fun MenuItem.clicks(
        scope: CoroutineScope,
        handled: (MenuItem) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    setOnMenuItemClickListener(listener(handled = handled, emitter = ::offer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

// -----------------------------------------------------------------------------------------------

private fun listener(
        handled: (MenuItem) -> Boolean,
        emitter: (MenuItem) -> Boolean
) = MenuItem.OnMenuItemClickListener { item ->
    if (handled(item)) { emitter(item) }
    else { false }
}
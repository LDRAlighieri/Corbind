package ru.ldralighieri.corbind.view

import android.view.MenuItem
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------

fun MenuItem.clicks(
        handled: (MenuItem) -> Boolean = AlwaysTrue,
        action: suspend (MenuItem) -> Unit
) {
    val events = actor<MenuItem>(Dispatchers.Main, capacity = Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener { item ->
        if (handled(item)) { events.offer(item) }
        else { false }
    }
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}
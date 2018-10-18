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

    setOnMenuItemClickListener { item ->
        if (handled(item)) { events.offer(item) }
        else { false }
    }

    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

// -----------------------------------------------------------------------------------------------

fun MenuItem.clicks(
        scope: CoroutineScope,
        handled: (MenuItem) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, capacity = Channel.CONFLATED) {
    setOnMenuItemClickListener { item ->
        if (handled(item)) { offer(item); }
        else { false }
    }

    invokeOnClose { setOnMenuItemClickListener(null) }
}
package ru.ldralighieri.corbind.appcompat

import android.view.MenuItem
import androidx.appcompat.widget.ActionMenuView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun ActionMenuView.itemClicks(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

suspend fun ActionMenuView.itemClicks(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {
    val events = actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun ActionMenuView.itemClicks(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnMenuItemClickListener(listener(::offer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

suspend fun ActionMenuView.itemClicks(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setOnMenuItemClickListener(listener(::offer))
        invokeOnClose { setOnMenuItemClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (MenuItem) -> Boolean
) = ActionMenuView.OnMenuItemClickListener(emitter::invoke)
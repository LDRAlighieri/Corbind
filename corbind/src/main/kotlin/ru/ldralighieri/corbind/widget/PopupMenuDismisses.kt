package ru.ldralighieri.corbind.widget

import android.widget.PopupMenu
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun PopupMenu.dismisses(
        scope: CoroutineScope,
        action: suspend (PopupMenu) -> Unit
) {
    val events = scope.actor<PopupMenu>(Dispatchers.Main, Channel.CONFLATED) {
        for (menu in channel) action(menu)
    }

    setOnDismissListener(listener(events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}

suspend fun PopupMenu.dismisses(
        action: suspend (PopupMenu) -> Unit
) = coroutineScope {
    val events = actor<PopupMenu>(Dispatchers.Main, Channel.CONFLATED) {
        for (menu in channel) action(menu)
    }

    setOnDismissListener(listener(events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun PopupMenu.dismisses(
        scope: CoroutineScope
): ReceiveChannel<PopupMenu> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnDismissListener(listener(::offer))
    invokeOnClose { setOnDismissListener(null) }
}

suspend fun PopupMenu.dismisses(): ReceiveChannel<PopupMenu> = coroutineScope {

    produce<PopupMenu>(Dispatchers.Main, Channel.CONFLATED) {
        setOnDismissListener(listener(::offer))
        invokeOnClose { setOnDismissListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (PopupMenu) -> Boolean
) = PopupMenu.OnDismissListener { emitter(it) }
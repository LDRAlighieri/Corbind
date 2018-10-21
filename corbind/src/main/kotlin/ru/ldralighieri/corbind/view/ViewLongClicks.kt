package ru.ldralighieri.corbind.view

import android.view.View
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------


fun View.longClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (click in channel) action(click)
    }

    setOnLongClickListener(listener(handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}

suspend fun View.longClicks(
        handled: () -> Boolean = AlwaysTrue,
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (click in channel) action(click)
    }

    setOnLongClickListener(listener(handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun View.longClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnLongClickListener(listener(handled, ::offer))
    invokeOnClose { setOnLongClickListener(null) }
}

suspend fun View.longClicks(
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        setOnLongClickListener(listener(handled, ::offer))
        invokeOnClose { setOnLongClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        handled: () -> Boolean,
        emitter: (View) -> Boolean
) = View.OnLongClickListener {
    if (handled()) { emitter(it) }
    else { false }
}
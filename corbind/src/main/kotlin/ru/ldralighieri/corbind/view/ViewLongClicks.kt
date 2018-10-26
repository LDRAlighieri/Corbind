package ru.ldralighieri.corbind.view

import android.view.View
import androidx.annotation.CheckResult
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
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnLongClickListener(listener(handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}

suspend fun View.longClicks(
        handled: () -> Boolean = AlwaysTrue,
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnLongClickListener(listener(handled, events::offer))
    events.invokeOnClose { setOnLongClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun View.longClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnLongClickListener(listener(handled, ::offer))
    invokeOnClose { setOnLongClickListener(null) }
}

@CheckResult
suspend fun View.longClicks(
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setOnLongClickListener(listener(handled, ::offer))
        invokeOnClose { setOnLongClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        handled: () -> Boolean,
        emitter: (Unit) -> Boolean
) = View.OnLongClickListener {
    if (handled()) { emitter(Unit) }
    else { false }
}
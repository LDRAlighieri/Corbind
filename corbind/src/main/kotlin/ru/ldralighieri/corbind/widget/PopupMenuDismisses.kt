package ru.ldralighieri.corbind.widget

import android.widget.PopupMenu
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------


fun PopupMenu.dismisses(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}

suspend fun PopupMenu.dismisses(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun PopupMenu.dismisses(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnDismissListener(listener(this, ::offer))
    invokeOnClose { setOnDismissListener(null) }
}

@CheckResult
suspend fun PopupMenu.dismisses(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setOnDismissListener(listener(this, ::offer))
        invokeOnClose { setOnDismissListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = PopupMenu.OnDismissListener {

    if (scope.isActive) { emitter(Unit) }
}
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.appcompat

import androidx.annotation.CheckResult
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun PopupMenu.dismisses(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

suspend fun PopupMenu.dismisses(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun PopupMenu.dismisses(
        scope: CoroutineScope
): ReceiveChannel<Unit> = corbindReceiveChannel {

    setOnDismissListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

@CheckResult
suspend fun PopupMenu.dismisses(): ReceiveChannel<Unit> = coroutineScope {

    corbindReceiveChannel<Unit> {
        setOnDismissListener(listener(this@coroutineScope, ::safeOffer))
        invokeOnClose { setOnMenuItemClickListener(null) }
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
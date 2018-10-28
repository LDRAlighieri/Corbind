package ru.ldralighieri.corbind.appcompat

import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------


fun Toolbar.itemClicks(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

suspend fun Toolbar.itemClicks(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun Toolbar.itemClicks(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnMenuItemClickListener(listener(this, ::offer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}

@CheckResult
suspend fun Toolbar.itemClicks(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setOnMenuItemClickListener(listener(this, ::offer))
        invokeOnClose { setOnMenuItemClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (MenuItem) -> Boolean
) = Toolbar.OnMenuItemClickListener {

    if (scope.isActive) { emitter(it) }
    return@OnMenuItemClickListener true
}
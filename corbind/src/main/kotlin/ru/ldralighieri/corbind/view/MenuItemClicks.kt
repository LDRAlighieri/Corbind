@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.view

import android.view.MenuItem
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------


fun MenuItem.clicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItem) -> Boolean = AlwaysTrue,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

suspend fun MenuItem.clicks(
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItem) -> Boolean = AlwaysTrue,
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener(listener(this, handled, events::offer))
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun MenuItem.clicks(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        handled: (MenuItem) -> Boolean = AlwaysTrue
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {

    setOnMenuItemClickListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        handled: (MenuItem) -> Boolean,
        emitter: (MenuItem) -> Boolean
) = MenuItem.OnMenuItemClickListener { item ->

    if (scope.isActive) {
        if (handled(item)) {
            emitter(item)
            return@OnMenuItemClickListener true
        }
    }

    return@OnMenuItemClickListener false
}
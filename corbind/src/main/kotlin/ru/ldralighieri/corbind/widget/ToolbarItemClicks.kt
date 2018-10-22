package ru.ldralighieri.corbind.widget

import android.os.Build
import android.view.MenuItem
import android.widget.Toolbar
import androidx.annotation.RequiresApi
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.itemClicks(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener { events.offer(it) }
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.itemClicks(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {
    val events = actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setOnMenuItemClickListener { events.offer(it) }
    events.invokeOnClose { setOnMenuItemClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.itemClicks(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnMenuItemClickListener { offer(it) }
    invokeOnClose { setOnMenuItemClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.itemClicks(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setOnMenuItemClickListener { offer(it) }
        invokeOnClose { setOnMenuItemClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (MenuItem) -> Boolean
) = Toolbar.OnMenuItemClickListener { emitter(it) }
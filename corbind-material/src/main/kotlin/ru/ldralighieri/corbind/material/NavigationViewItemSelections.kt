package ru.ldralighieri.corbind.material

import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun NavigationView.itemSelections(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events::offer)
    setNavigationItemSelectedListener(listener(events::offer))
    events.invokeOnClose { setNavigationItemSelectedListener(null) }
}

suspend fun NavigationView.itemSelections(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {
    val events = actor<MenuItem>(UI, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setInitialValue(this@itemSelections, events::offer)
    setNavigationItemSelectedListener(listener(events::offer))
    events.invokeOnClose { setNavigationItemSelectedListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun NavigationView.itemSelections(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setInitialValue(this@itemSelections, ::offer)
    setNavigationItemSelectedListener(listener(::offer))
    invokeOnClose { setNavigationItemSelectedListener(null) }
}

suspend fun NavigationView.itemSelections(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setInitialValue(this@itemSelections, ::offer)
        setNavigationItemSelectedListener(listener(::offer))
        invokeOnClose { setNavigationItemSelectedListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun setInitialValue(
        navigationView: NavigationView,
        emitter: (MenuItem) -> Boolean
) {
    val menu = navigationView.menu
    for (i in 0 until menu.size()) {
        val item = menu.getItem(i)
        if (item.isChecked) {
            emitter(item)
            break
        }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (MenuItem) -> Boolean
) = NavigationView.OnNavigationItemSelectedListener(emitter::invoke)
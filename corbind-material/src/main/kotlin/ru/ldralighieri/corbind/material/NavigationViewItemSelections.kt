@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import android.view.MenuItem
import androidx.annotation.CheckResult
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun NavigationView.itemSelections(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events::offer)
    setNavigationItemSelectedListener(listener(scope, events::offer))
    events.invokeOnClose { setNavigationItemSelectedListener(null) }
}

suspend fun NavigationView.itemSelections(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setInitialValue(this@itemSelections, events::offer)
    setNavigationItemSelectedListener(listener(this, events::offer))
    events.invokeOnClose { setNavigationItemSelectedListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun NavigationView.itemSelections(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setInitialValue(this@itemSelections, ::offer)
    setNavigationItemSelectedListener(listener(this, ::offer))
    invokeOnClose { setNavigationItemSelectedListener(null) }
}

@CheckResult
suspend fun NavigationView.itemSelections(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setInitialValue(this@itemSelections, ::offer)
        setNavigationItemSelectedListener(listener(this, ::offer))
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


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (MenuItem) -> Boolean
) = NavigationView.OnNavigationItemSelectedListener {

    if (scope.isActive) { emitter(it) }
    return@OnNavigationItemSelectedListener true
}
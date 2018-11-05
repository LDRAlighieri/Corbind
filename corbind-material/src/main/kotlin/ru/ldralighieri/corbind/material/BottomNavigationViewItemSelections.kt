@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import android.view.MenuItem
import androidx.annotation.CheckResult
import com.google.android.material.bottomnavigation.BottomNavigationView
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


fun BottomNavigationView.itemSelections(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (MenuItem) -> Unit
) {

    val events = scope.actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events::offer)
    setOnNavigationItemSelectedListener(listener(scope, events::offer))
    events.invokeOnClose { setOnNavigationItemSelectedListener(null) }
}

suspend fun BottomNavigationView.itemSelections(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (MenuItem) -> Unit
) = coroutineScope {

    val events = actor<MenuItem>(Dispatchers.Main, capacity) {
        for (item in channel) action(item)
    }

    setInitialValue(this@itemSelections, events::offer)
    setOnNavigationItemSelectedListener(listener(this, events::offer))
    events.invokeOnClose { setOnNavigationItemSelectedListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun BottomNavigationView.itemSelections(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<MenuItem> = corbindReceiveChannel(capacity) {

    setInitialValue(this@itemSelections, ::safeOffer)
    setOnNavigationItemSelectedListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnNavigationItemSelectedListener(null) }
}


// -----------------------------------------------------------------------------------------------


private fun setInitialValue(
        bottomNavigationView: BottomNavigationView,
        emitter: (MenuItem) -> Boolean
) {
    val menu = bottomNavigationView.menu
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
) = BottomNavigationView.OnNavigationItemSelectedListener {

    if (scope.isActive) { emitter(it) }
    return@OnNavigationItemSelectedListener true
}
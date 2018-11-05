@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.drawerlayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.drawerlayout.widget.DrawerLayout
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


fun DrawerLayout.drawerOpens(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        gravity: Int,
        action: suspend (Boolean) -> Unit
) {

    val events = scope.actor<Boolean>(Dispatchers.Main, capacity) {
        for (open in channel) action(open)
    }

    events.offer(isDrawerOpen(gravity))
    val listener = listener(scope, gravity, events::offer)
    addDrawerListener(listener)
    events.invokeOnClose { removeDrawerListener(listener) }
}

suspend fun DrawerLayout.drawerOpens(
        capacity: Int = Channel.RENDEZVOUS,
        gravity: Int,
        action: suspend (Boolean) -> Unit
) = coroutineScope {

    val events = actor<Boolean>(Dispatchers.Main, capacity) {
        for (open in channel) action(open)
    }

    events.offer(isDrawerOpen(gravity))
    val listener = listener(this, gravity, events::offer)
    addDrawerListener(listener)
    events.invokeOnClose { removeDrawerListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun DrawerLayout.drawerOpens(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        gravity: Int
): ReceiveChannel<Boolean> = corbindReceiveChannel(capacity) {

    safeOffer(isDrawerOpen(gravity))
    val listener = listener(scope, gravity, ::safeOffer)
    addDrawerListener(listener)
    invokeOnClose { removeDrawerListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        gravity: Int,
        emitter: (Boolean) -> Boolean
) = object : DrawerLayout.DrawerListener {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {  }
    override fun onDrawerOpened(drawerView: View) { onEvent(drawerView, true) }
    override fun onDrawerClosed(drawerView: View) { onEvent(drawerView, false) }
    override fun onDrawerStateChanged(newState: Int) {  }

    private fun onEvent(drawerView: View, opened: Boolean) {
        if (scope.isActive) {
            val drawerGravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
            if (drawerGravity == gravity) { emitter(opened) }
        }
    }
}
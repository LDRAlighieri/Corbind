package ru.ldralighieri.corbind.drawerlayout

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun DrawerLayout.drawerOpen(
        scope: CoroutineScope,
        gravity: Int,
        action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (open in channel) action(open)
    }

    events.offer(isDrawerOpen(gravity))
    val listener = listener(gravity, events::offer)
    addDrawerListener(listener)
    events.invokeOnClose { removeDrawerListener(listener) }
}

suspend fun DrawerLayout.drawerOpen(
        gravity: Int,
        action: suspend (Boolean) -> Unit
) = coroutineScope {
    val events = actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (open in channel) action(open)
    }

    events.offer(isDrawerOpen(gravity))
    val listener = listener(gravity, events::offer)
    addDrawerListener(listener)
    events.invokeOnClose { removeDrawerListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun DrawerLayout.drawerOpen(
        scope: CoroutineScope,
        gravity: Int
): ReceiveChannel<Boolean> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(isDrawerOpen(gravity))
    val listener = listener(gravity, ::offer)
    addDrawerListener(listener)
    invokeOnClose { removeDrawerListener(listener) }
}

suspend fun DrawerLayout.drawerOpen(
        gravity: Int
): ReceiveChannel<Boolean> = coroutineScope {

    produce<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        offer(isDrawerOpen(gravity))
        val listener = listener(gravity, ::offer)
        addDrawerListener(listener)
        invokeOnClose { removeDrawerListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        gravity: Int,
        emitter: (Boolean) -> Boolean
) = object : DrawerLayout.DrawerListener {

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {  }

    override fun onDrawerOpened(drawerView: View) {
        val drawerGravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
        if (drawerGravity == gravity) { emitter(true) }
    }

    override fun onDrawerClosed(drawerView: View) {
        val drawerGravity = (drawerView.layoutParams as DrawerLayout.LayoutParams).gravity
        if (drawerGravity == gravity) { emitter(false) }
    }

    override fun onDrawerStateChanged(newState: Int) {  }
}
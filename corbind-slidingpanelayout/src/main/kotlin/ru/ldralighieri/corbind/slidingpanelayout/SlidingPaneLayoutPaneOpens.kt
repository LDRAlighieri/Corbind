package ru.ldralighieri.corbind.slidingpanelayout

import android.view.View
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun SlidingPaneLayout.panelOpens(
        scope: CoroutineScope,
        action: suspend (Boolean) -> Unit
) {
    val events = scope.actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(isOpen)
    setPanelSlideListener(listener(events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}

suspend fun SlidingPaneLayout.panelOpens(
        action: suspend (Boolean) -> Unit
) = coroutineScope {
    val events = actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(isOpen)
    setPanelSlideListener(listener(events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SlidingPaneLayout.panelOpens(
        scope: CoroutineScope
): ReceiveChannel<Boolean> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(isOpen)
    setPanelSlideListener(listener(::offer))
    invokeOnClose { setPanelSlideListener(null) }
}

suspend fun SlidingPaneLayout.panelOpens(): ReceiveChannel<Boolean> = coroutineScope {

    produce<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        offer(isOpen)
        setPanelSlideListener(listener(::offer))
        invokeOnClose { setPanelSlideListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (Boolean) -> Boolean
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) {  }
    override fun onPanelOpened(panel: View) { emitter(true) }
    override fun onPanelClosed(panel: View) { emitter(false) }
}
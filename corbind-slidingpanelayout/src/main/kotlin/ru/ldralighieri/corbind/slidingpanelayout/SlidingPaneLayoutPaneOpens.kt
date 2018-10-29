@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.slidingpanelayout

import android.view.View
import androidx.annotation.CheckResult
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun SlidingPaneLayout.panelOpens(
        scope: CoroutineScope,
        action: suspend (Boolean) -> Unit
) {

    val events = scope.actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(isOpen)
    setPanelSlideListener(listener(scope, events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}

suspend fun SlidingPaneLayout.panelOpens(
        action: suspend (Boolean) -> Unit
) = coroutineScope {

    val events = actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(isOpen)
    setPanelSlideListener(listener(this, events::offer))
    events.invokeOnClose { setPanelSlideListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SlidingPaneLayout.panelOpens(
        scope: CoroutineScope
): ReceiveChannel<Boolean> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(isOpen)
    setPanelSlideListener(listener(this, ::offer))
    invokeOnClose { setPanelSlideListener(null) }
}

@CheckResult
suspend fun SlidingPaneLayout.panelOpens(): ReceiveChannel<Boolean> = coroutineScope {

    produce<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        offer(isOpen)
        setPanelSlideListener(listener(this, ::offer))
        invokeOnClose { setPanelSlideListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Boolean) -> Boolean
) = object : SlidingPaneLayout.PanelSlideListener {

    override fun onPanelSlide(panel: View, slideOffset: Float) {  }
    override fun onPanelOpened(panel: View) { onEvent(true) }
    override fun onPanelClosed(panel: View) { onEvent(false) }

    private fun onEvent(event: Boolean) {
        if (scope.isActive) { emitter(event) }
    }
}
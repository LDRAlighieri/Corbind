package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

sealed class TabLayoutSelectionEvent {
    abstract val view: TabLayout
    abstract val tab: TabLayout.Tab
}

data class TabLayoutSelectionSelectedEvent(
        override val view: TabLayout,
        override val tab: TabLayout.Tab
) : TabLayoutSelectionEvent()

data class TabLayoutSelectionReselectedEvent(
        override val view: TabLayout,
        override val tab: TabLayout.Tab
) : TabLayoutSelectionEvent()

data class TabLayoutSelectionUnselectedEvent(
        override val view: TabLayout,
        override val tab: TabLayout.Tab
) : TabLayoutSelectionEvent()

// -----------------------------------------------------------------------------------------------


fun TabLayout.selectionEvents(
        scope: CoroutineScope,
        action: suspend (TabLayoutSelectionEvent) -> Unit
) {
    val events = scope.actor<TabLayoutSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setInitialValue(this, events::offer)
    val listener = listener(this, events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}

suspend fun TabLayout.selectionEvents(
        action: suspend (TabLayoutSelectionEvent) -> Unit
) = coroutineScope {
    val events = actor<TabLayoutSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setInitialValue(this@selectionEvents, events::offer)
    val listener = listener(this@selectionEvents, events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TabLayout.selectionEvents(
        scope: CoroutineScope
): ReceiveChannel<TabLayoutSelectionEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setInitialValue(this@selectionEvents, ::offer)
    val listener = listener(this@selectionEvents, ::offer)
    addOnTabSelectedListener(listener)
    invokeOnClose { removeOnTabSelectedListener(listener) }
}

@CheckResult
suspend fun TabLayout.selectionEvents(): ReceiveChannel<TabLayoutSelectionEvent> = coroutineScope {

    produce<TabLayoutSelectionEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setInitialValue(this@selectionEvents, ::offer)
        val listener = listener(this@selectionEvents, ::offer)
        addOnTabSelectedListener(listener)
        invokeOnClose { removeOnTabSelectedListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun setInitialValue(
        tabLayout: TabLayout,
        emitter: (TabLayoutSelectionEvent) -> Boolean
) {
    val index = tabLayout.selectedTabPosition
    if (index != -1) {
        emitter(TabLayoutSelectionSelectedEvent(tabLayout, tabLayout.getTabAt(index)!!))
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        tabLayout: TabLayout,
        emitter: (TabLayoutSelectionEvent) -> Boolean
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) {
        emitter(TabLayoutSelectionSelectedEvent(tabLayout, tab))
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        emitter(TabLayoutSelectionUnselectedEvent(tabLayout, tab))
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        emitter(TabLayoutSelectionReselectedEvent(tabLayout, tab))
    }
}
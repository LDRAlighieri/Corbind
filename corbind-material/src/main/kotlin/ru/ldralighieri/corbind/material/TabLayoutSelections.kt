package ru.ldralighieri.corbind.material

import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun TabLayout.selections(
        scope: CoroutineScope,
        action: suspend (TabLayout.Tab) -> Unit
) {
    val events = scope.actor<TabLayout.Tab>(Dispatchers.Main, Channel.CONFLATED) {
        for (tab in channel) action(tab)
    }

    setInitialValue(this, events::offer)
    val listener = listener(events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}

suspend fun TabLayout.selections(
        action: suspend (TabLayout.Tab) -> Unit
) = coroutineScope {
    val events = actor<TabLayout.Tab>(Dispatchers.Main, Channel.CONFLATED) {
        for (tab in channel) action(tab)
    }

    setInitialValue(this@selections, events::offer)
    val listener = listener(events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun TabLayout.selections(
        scope: CoroutineScope
): ReceiveChannel<TabLayout.Tab> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setInitialValue(this@selections, ::offer)
    val listener = listener(::offer)
    addOnTabSelectedListener(listener)
    invokeOnClose { removeOnTabSelectedListener(listener) }
}

suspend fun TabLayout.selections(): ReceiveChannel<TabLayout.Tab> = coroutineScope {

    produce<TabLayout.Tab>(Dispatchers.Main, Channel.CONFLATED) {
        setInitialValue(this@selections, ::offer)
        val listener = listener(::offer)
        addOnTabSelectedListener(listener)
        invokeOnClose { removeOnTabSelectedListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun setInitialValue(
        tabLayout: TabLayout,
        emitter: (TabLayout.Tab) -> Boolean
) {
    val index = tabLayout.selectedTabPosition
    if (index != -1) { emitter(tabLayout.getTabAt(index)!!) }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (TabLayout.Tab) -> Boolean
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) { emitter(tab) }
    override fun onTabUnselected(tab: TabLayout.Tab) {  }
    override fun onTabReselected(tab: TabLayout.Tab) {  }

}
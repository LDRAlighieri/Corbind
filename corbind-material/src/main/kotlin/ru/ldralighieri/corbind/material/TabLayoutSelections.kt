@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive

// -----------------------------------------------------------------------------------------------


fun TabLayout.selections(
        scope: CoroutineScope,
        action: suspend (TabLayout.Tab) -> Unit
) {

    val events = scope.actor<TabLayout.Tab>(Dispatchers.Main, Channel.CONFLATED) {
        for (tab in channel) action(tab)
    }

    setInitialValue(this, events::offer)
    val listener = listener(scope, events::offer)
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
    val listener = listener(this, events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun TabLayout.selections(
        scope: CoroutineScope
): ReceiveChannel<TabLayout.Tab> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setInitialValue(this@selections, ::offer)
    val listener = listener(this, ::offer)
    addOnTabSelectedListener(listener)
    invokeOnClose { removeOnTabSelectedListener(listener) }
}

@CheckResult
suspend fun TabLayout.selections(): ReceiveChannel<TabLayout.Tab> = coroutineScope {

    produce<TabLayout.Tab>(Dispatchers.Main, Channel.CONFLATED) {
        setInitialValue(this@selections, ::offer)
        val listener = listener(this, ::offer)
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

@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (TabLayout.Tab) -> Boolean
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) {
        if (scope.isActive) { emitter(tab) }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {  }
    override fun onTabReselected(tab: TabLayout.Tab) {  }

}
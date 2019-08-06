@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.material

import androidx.annotation.CheckResult
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

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


/**
 * Perform an action on selection, reselection, and unselection events for the tabs in [TabLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TabLayout.selectionEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (TabLayoutSelectionEvent) -> Unit
) {

    val events = scope.actor<TabLayoutSelectionEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setInitialValue(this, events::offer)
    val listener = listener(scope = scope, tabLayout = this, emitter = events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}


/**
 * Perform an action on selection, reselection, and unselection events for the tabs in [TabLayout]
 * inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TabLayout.selectionEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (TabLayoutSelectionEvent) -> Unit
) = coroutineScope {

    val events = actor<TabLayoutSelectionEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setInitialValue(this@selectionEvents, events::offer)
    val listener = listener(scope = this, tabLayout = this@selectionEvents, emitter = events::offer)
    addOnTabSelectedListener(listener)
    events.invokeOnClose { removeOnTabSelectedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel which emits selection, reselection, and unselection events for the tabs in
 * [TabLayout].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TabLayout.selectionEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TabLayoutSelectionEvent> = corbindReceiveChannel(capacity) {
    setInitialValue(this@selectionEvents, ::safeOffer)
    val listener = listener(scope, this@selectionEvents, ::safeOffer)
    addOnTabSelectedListener(listener)
    invokeOnClose { removeOnTabSelectedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow which emits selection, reselection, and unselection events for the tabs in
 * [TabLayout].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
fun TabLayout.selectionEvents(): Flow<TabLayoutSelectionEvent> = channelFlow {
    setInitialValue(this@selectionEvents, ::offer)
    val listener = listener(this, this@selectionEvents, ::offer)
    addOnTabSelectedListener(listener)
    awaitClose { removeOnTabSelectedListener(listener) }
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
        scope: CoroutineScope,
        tabLayout: TabLayout,
        emitter: (TabLayoutSelectionEvent) -> Boolean
) = object : TabLayout.OnTabSelectedListener {

    override fun onTabSelected(tab: TabLayout.Tab) {
        onEvent(TabLayoutSelectionSelectedEvent(tabLayout, tab))
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
        onEvent(TabLayoutSelectionUnselectedEvent(tabLayout, tab))
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        onEvent(TabLayoutSelectionReselectedEvent(tabLayout, tab))
    }

    private fun onEvent(event: TabLayoutSelectionEvent) {
        if (scope.isActive) { emitter(event) }
    }

}

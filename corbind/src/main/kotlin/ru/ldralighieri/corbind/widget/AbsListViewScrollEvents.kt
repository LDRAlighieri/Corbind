@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.AbsListView
import androidx.annotation.CheckResult
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

/**
 * A list view on scroll event
 */
data class AbsListViewScrollEvent(
        val view: AbsListView,
        val scrollState: Int,
        val firstVisibleItem: Int,
        val visibleItemCount: Int,
        val totalItemCount: Int
)

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action on scroll events on `absListView`.
 */
fun AbsListView.scrollEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (AbsListViewScrollEvent) -> Unit
) {

    val events = scope.actor<AbsListViewScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollListener(listener(scope, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}

/**
 * Perform an action on scroll events on `absListView` inside new CoroutineScope.
 */
suspend fun AbsListView.scrollEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (AbsListViewScrollEvent) -> Unit
) = coroutineScope {

    val events = actor<AbsListViewScrollEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    setOnScrollListener(listener(this, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of scroll events on `absListView`.
 */
@CheckResult
fun AbsListView.scrollEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<AbsListViewScrollEvent> = corbindReceiveChannel(capacity) {
    setOnScrollListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnScrollListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a flow of scroll events on `absListView`.
 */
@CheckResult
fun AbsListView.scrollEvents(): Flow<AbsListViewScrollEvent> = channelFlow {
    setOnScrollListener(listener(this, ::offer))
    awaitClose { setOnScrollListener(null) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Listener of `absListView` scroll events.
 */
@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (AbsListViewScrollEvent) -> Boolean
) = object : AbsListView.OnScrollListener {

    private var currentScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE

    override fun onScrollStateChanged(absListView: AbsListView, scrollState: Int) {
        currentScrollState = scrollState
        if (scope.isActive) {
            val event = AbsListViewScrollEvent(absListView, scrollState,
                    absListView.firstVisiblePosition, absListView.childCount, absListView.count)
            emitter(event)
        }
    }

    override fun onScroll(
            absListView: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
            totalItemCount: Int
    ) {
        if (scope.isActive) {
            val event = AbsListViewScrollEvent(absListView, currentScrollState, firstVisibleItem,
                    visibleItemCount, totalItemCount)
            emitter(event)
        }
    }

}

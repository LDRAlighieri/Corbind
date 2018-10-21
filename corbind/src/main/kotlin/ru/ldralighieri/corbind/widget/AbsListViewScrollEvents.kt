package ru.ldralighieri.corbind.widget

import android.widget.AbsListView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------

data class AbsListViewScrollEvent(
        val view: AbsListView,
        val scrollState: Int,
        val firstVisibleItem: Int,
        val visibleItemCount: Int,
        val totalItemCount: Int
)

// -----------------------------------------------------------------------------------------------


fun AbsListView.scrollEvents(
        scope: CoroutineScope,
        action: suspend (AbsListViewScrollEvent) -> Unit
) {
    val events = scope.actor<AbsListViewScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnScrollListener(listener(this, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}

suspend fun AbsListView.scrollEvents(
        action: suspend (AbsListViewScrollEvent) -> Unit
) = coroutineScope {
    val events = actor<AbsListViewScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    setOnScrollListener(listener(this@scrollEvents, events::offer))
    events.invokeOnClose { setOnScrollListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun AbsListView.scrollEvents(
        scope: CoroutineScope
): ReceiveChannel<AbsListViewScrollEvent> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnScrollListener(listener(this@scrollEvents, ::offer))
    invokeOnClose { setOnScrollListener(null) }
}

suspend fun AbsListView.scrollEvents(): ReceiveChannel<AbsListViewScrollEvent> = coroutineScope {

    produce<AbsListViewScrollEvent>(Dispatchers.Main, Channel.CONFLATED) {
        setOnScrollListener(listener(this@scrollEvents, ::offer))
        invokeOnClose { setOnScrollListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        view: AbsListView,
        emitter: (AbsListViewScrollEvent) -> Boolean
) = object : AbsListView.OnScrollListener {
    private var currentScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE

    override fun onScrollStateChanged(absListView: AbsListView, scrollState: Int) {
        currentScrollState = scrollState
        val event = AbsListViewScrollEvent(view, scrollState, view.firstVisiblePosition,
                view.childCount, view.count)
        emitter(event)
    }

    override fun onScroll(
            absListView: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
            totalItemCount: Int
    ) {
        val event = AbsListViewScrollEvent(view, currentScrollState, firstVisibleItem,
                visibleItemCount, totalItemCount)
        emitter(event)
    }
}


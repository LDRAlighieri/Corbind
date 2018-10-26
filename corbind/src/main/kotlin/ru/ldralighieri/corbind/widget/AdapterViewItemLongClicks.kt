package ru.ldralighieri.corbind.widget

import android.widget.Adapter
import android.widget.AdapterView
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import ru.ldralighieri.corbind.internal.AlwaysTrue

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemLongClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main,Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    onItemLongClickListener = listener(handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemLongClicks(
        handled: () -> Boolean = AlwaysTrue,
        action: suspend (Int) -> Unit
) = coroutineScope {
    val events = actor<Int>(Dispatchers.Main,Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    onItemLongClickListener = listener(handled, events::offer)
    events.invokeOnClose { onItemLongClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.itemLongClicks(
        scope: CoroutineScope,
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    onItemLongClickListener = listener(handled, ::offer)
    invokeOnClose { onItemLongClickListener = null }
}

@CheckResult
suspend fun <T : Adapter> AdapterView<T>.itemLongClicks(
        handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        onItemLongClickListener = listener(handled, ::offer)
        invokeOnClose { onItemLongClickListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        handled: () -> Boolean,
        emitter: (Int) -> Boolean
) = AdapterView.OnItemLongClickListener { _, _, position, _ ->
    if (handled()) { emitter(position) } else { false }
}
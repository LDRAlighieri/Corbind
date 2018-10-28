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
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------


fun <T : Adapter> AdapterView<T>.itemClicks(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    onItemClickListener = listener(scope, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}

suspend fun <T : Adapter> AdapterView<T>.itemClicks(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    onItemClickListener = listener(this, events::offer)
    events.invokeOnClose { onItemClickListener = null }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun <T : Adapter> AdapterView<T>.itemClicks(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    onItemClickListener = listener(this, ::offer)
    invokeOnClose { onItemClickListener = null }
}

@CheckResult
suspend fun <T : Adapter> AdapterView<T>.itemClicks(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        onItemClickListener = listener(this, ::offer)
        invokeOnClose { onItemClickListener = null }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = AdapterView.OnItemClickListener { _, _, position, _ ->

    if (scope.isActive) { emitter(position) }
}
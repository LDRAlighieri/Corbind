package ru.ldralighieri.corbind.widget

import android.os.Build
import android.view.View
import android.widget.Toolbar
import androidx.annotation.RequiresApi
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        action: suspend (View) -> Unit
) {
    val events = scope.actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    setNavigationOnClickListener(listener(events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.navigationClicks(
        action: suspend (View) -> Unit
) = coroutineScope {
    val events = actor<View>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    setNavigationOnClickListener(listener(events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.navigationClicks(
        scope: CoroutineScope
): ReceiveChannel<View> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setNavigationOnClickListener(listener(::offer))
    invokeOnClose { setNavigationOnClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.navigationClicks(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        setNavigationOnClickListener(listener(::offer))
        invokeOnClose { setNavigationOnClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (View) -> Boolean
) = View.OnClickListener { emitter(it) }
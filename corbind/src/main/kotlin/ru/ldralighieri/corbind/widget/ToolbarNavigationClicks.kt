package ru.ldralighieri.corbind.widget

import android.os.Build
import android.view.View
import android.widget.Toolbar
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.isActive

// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Toolbar.navigationClicks(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(scope, events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun Toolbar.navigationClicks(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setNavigationOnClickListener(listener(this, events::offer))
    events.invokeOnClose { setNavigationOnClickListener(null) }
}


// -----------------------------------------------------------------------------------------------


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
fun Toolbar.navigationClicks(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setNavigationOnClickListener(listener(this, ::offer))
    invokeOnClose { setNavigationOnClickListener(null) }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@CheckResult
suspend fun Toolbar.navigationClicks(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setNavigationOnClickListener(listener(this, ::offer))
        invokeOnClose { setNavigationOnClickListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = View.OnClickListener {

    if (scope.isActive) { emitter(Unit) }
}
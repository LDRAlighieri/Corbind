package ru.ldralighieri.corbind.widget

import android.widget.CompoundButton
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


fun CompoundButton.checkedChanges(
        scope: CoroutineScope,
        action: suspend (Boolean) -> Unit
) {

    val events = scope.actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (checked in channel) action(checked)
    }

    events.offer(isChecked)
    setOnCheckedChangeListener(listener(scope, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

suspend fun CompoundButton.checkedChanges(
        action: suspend (Boolean) -> Unit
) = coroutineScope {

    val events = actor<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        for (checked in channel) action(checked)
    }

    events.offer(isChecked)
    setOnCheckedChangeListener(listener(this, events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun CompoundButton.checkedChanges(
        scope: CoroutineScope
): ReceiveChannel<Boolean> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(isChecked)
    setOnCheckedChangeListener(listener(this, ::offer))
    invokeOnClose { setOnCheckedChangeListener(null) }
}

@CheckResult
suspend fun CompoundButton.checkedChanges(): ReceiveChannel<Boolean> = coroutineScope {

    produce<Boolean>(Dispatchers.Main, Channel.CONFLATED) {
        offer(isChecked)
        setOnCheckedChangeListener(listener(this, ::offer))
        invokeOnClose { setOnCheckedChangeListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Boolean) -> Boolean
) = CompoundButton.OnCheckedChangeListener { _, isChecked ->

    if (scope.isActive) { emitter(isChecked) }
}
@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.widget.CompoundButton
import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

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
): ReceiveChannel<Boolean> = corbindReceiveChannel {

    offer(isChecked)
    setOnCheckedChangeListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnCheckedChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Boolean) -> Boolean
) = CompoundButton.OnCheckedChangeListener { _, isChecked ->

    if (scope.isActive) { emitter(isChecked) }
}
package ru.ldralighieri.corbind.widget

import android.widget.RadioGroup
import androidx.annotation.CheckResult
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun RadioGroup.checkedChanges(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {
    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (checkedId in channel) action(checkedId)
    }

    events.offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}

suspend fun RadioGroup.checkedChanges(
        action: suspend (Int) -> Unit
) = coroutineScope {
    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (checkedId in channel) action(checkedId)
    }

    events.offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(events::offer))
    events.invokeOnClose { setOnCheckedChangeListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun RadioGroup.checkedChanges(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(checkedRadioButtonId)
    setOnCheckedChangeListener(listener(::offer))
    invokeOnClose { setOnCheckedChangeListener(null) }
}

@CheckResult
suspend fun RadioGroup.checkedChanges(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        offer(checkedRadioButtonId)
        setOnCheckedChangeListener(listener(::offer))
        invokeOnClose { setOnCheckedChangeListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        emitter: (Int) -> Boolean
) = object : RadioGroup.OnCheckedChangeListener {
    private var lastChecked = -1

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (checkedId != lastChecked) {
            lastChecked = checkedId
            emitter(checkedId)
        }
    }
}
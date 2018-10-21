package ru.ldralighieri.corbind.widget

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun TextView.textChanges(
        scope: CoroutineScope,
        action: suspend (CharSequence) -> Unit
) {
    val events = scope.actor<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(text)
    val listener = listener(events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.textChanges(
        action: suspend (CharSequence) -> Unit
) = coroutineScope {
    val events = actor<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        for (event in channel) action(event)
    }

    events.offer(text)
    val listener = listener(events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


fun TextView.textChanges(
        scope: CoroutineScope
): ReceiveChannel<CharSequence> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(text)
    val listener = listener(::offer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}

suspend fun TextView.textChanges(): ReceiveChannel<CharSequence> = coroutineScope {

    produce<CharSequence>(Dispatchers.Main, Channel.CONFLATED) {
        offer(text)
        val listener = listener(::offer)
        addTextChangedListener(listener)
        invokeOnClose { removeTextChangedListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (CharSequence) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {  }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { emitter(s) }
    override fun afterTextChanged(s: Editable) {  }
}
package ru.ldralighieri.corbind.leanback

import androidx.leanback.widget.SearchEditText
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun SearchEditText.keyboardDismisses(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnKeyboardDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}

suspend fun SearchEditText.keyboardDismisses(
        action: suspend () -> Unit
) = coroutineScope {
    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnKeyboardDismissListener(listener(this@keyboardDismisses, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SearchEditText.keyboardDismisses(
        scope: CoroutineScope
): ReceiveChannel<Unit> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnKeyboardDismissListener(listener(this@keyboardDismisses, ::offer))
    invokeOnClose { setOnKeyboardDismissListener(null) }
}

suspend fun SearchEditText.keyboardDismisses(): ReceiveChannel<Unit> = coroutineScope {

    produce<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        setOnKeyboardDismissListener(listener(this@keyboardDismisses, ::offer))
        invokeOnClose { setOnKeyboardDismissListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        searchEditText: SearchEditText,
        emitter: (Unit) -> Boolean
) = SearchEditText.OnKeyboardDismissListener { emitter(Unit) }
package ru.ldralighieri.corbind.leanback

import android.view.View
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
        action: suspend (SearchEditText) -> Unit
) {
    val events = scope.actor<SearchEditText>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    setOnKeyboardDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}

suspend fun SearchEditText.keyboardDismisses(
        action: suspend (SearchEditText) -> Unit
) = coroutineScope {
    val events = actor<SearchEditText>(Dispatchers.Main, Channel.CONFLATED) {
        for (view in channel) action(view)
    }

    setOnKeyboardDismissListener(listener(this@keyboardDismisses, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun SearchEditText.keyboardDismisses(
        scope: CoroutineScope
): ReceiveChannel<SearchEditText> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setOnKeyboardDismissListener(listener(this@keyboardDismisses, ::offer))
    invokeOnClose { setOnKeyboardDismissListener(null) }
}

suspend fun SearchEditText.keyboardDismisses(): ReceiveChannel<View> = coroutineScope {

    produce<View>(Dispatchers.Main, Channel.CONFLATED) {
        setOnKeyboardDismissListener(listener(this@keyboardDismisses, ::offer))
        invokeOnClose { setOnKeyboardDismissListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        searchEditText: SearchEditText,
        emitter: (SearchEditText) -> Boolean
) = SearchEditText.OnKeyboardDismissListener { emitter(searchEditText) }
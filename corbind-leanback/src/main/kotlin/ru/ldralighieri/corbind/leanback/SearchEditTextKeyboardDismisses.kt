@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.leanback

import androidx.annotation.CheckResult
import androidx.leanback.widget.SearchEditText
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


fun SearchEditText.keyboardDismisses(
        scope: CoroutineScope,
        action: suspend () -> Unit
) {

    val events = scope.actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnKeyboardDismissListener(listener(scope, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}

suspend fun SearchEditText.keyboardDismisses(
        action: suspend () -> Unit
) = coroutineScope {

    val events = actor<Unit>(Dispatchers.Main, Channel.CONFLATED) {
        for (unit in channel) action()
    }

    setOnKeyboardDismissListener(listener(this, events::offer))
    events.invokeOnClose { setOnKeyboardDismissListener(null) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun SearchEditText.keyboardDismisses(
        scope: CoroutineScope
): ReceiveChannel<Unit> = corbindReceiveChannel {

    setOnKeyboardDismissListener(listener(scope, ::safeOffer))
    invokeOnClose { setOnKeyboardDismissListener(null) }
}

@CheckResult
suspend fun SearchEditText.keyboardDismisses(): ReceiveChannel<Unit> = coroutineScope {

    corbindReceiveChannel<Unit> {
        setOnKeyboardDismissListener(listener(this@coroutineScope, ::safeOffer))
        invokeOnClose { setOnKeyboardDismissListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Unit) -> Boolean
) = SearchEditText.OnKeyboardDismissListener {

    if (scope.isActive) { emitter(Unit) }
}
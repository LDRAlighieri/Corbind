/*
 * Copyright 2026 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.widget

import android.os.Build
import android.text.TextWatcher
import android.widget.TextView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class TextViewInitialValueFlowTest {

    @Test
    fun `initial value is read when collection starts`() = runBlocking {
        val textView = TextView(RuntimeEnvironment.getApplication()).apply { text = "before" }
        val flow = textView.textChanges()

        textView.text = "after"

        val value = withTimeout(5_000) { flow.first().toString() }
        assertEquals("after", value)
    }

    @Test
    fun `each collection reads a fresh initial value`() = runBlocking {
        val textView = TextView(RuntimeEnvironment.getApplication())
        val flow = textView.textChanges()

        textView.text = "first"
        val firstValue = withTimeout(5_000) { flow.first().toString() }
        assertEquals("first", firstValue)

        textView.text = "second"
        val secondValue = withTimeout(5_000) { flow.first().toString() }
        assertEquals("second", secondValue)
    }

    @Test
    fun `listener receives changes while initial value is being collected`() = runBlocking {
        val textView = TextView(RuntimeEnvironment.getApplication()).apply { text = "initial" }
        val initialValueCollected = CompletableDeferred<Unit>()
        val continueCollecting = CompletableDeferred<Unit>()

        val values = withTimeout(5_000) {
            coroutineScope {
                val collection = async(start = CoroutineStart.UNDISPATCHED) {
                    textView.textChanges()
                        .onEach {
                            if (!initialValueCollected.isCompleted) {
                                initialValueCollected.complete(Unit)
                                continueCollecting.await()
                            }
                        }
                        .take(2)
                        .map(CharSequence::toString)
                        .toList()
                }

                initialValueCollected.await()
                textView.text = "changed"
                continueCollecting.complete(Unit)
                collection.await()
            }
        }

        assertEquals(listOf("initial", "changed"), values)
    }

    @Test
    fun `drop initial value keeps a callback emitted during registration`() = runBlocking {
        val textView = SynchronousCallbackTextView().apply { text = "initial" }

        val value = withTimeout(5_000) {
            textView.textChanges().dropInitialValue().first().toString()
        }

        assertEquals("callback", value)
    }

    @Test
    fun `collecting only the initial value still cleans up the listener`() = runBlocking {
        val textView = TrackingTextView().apply { text = "initial" }

        withTimeout(5_000) { textView.textChanges().first() }

        assertEquals(1, textView.addedListenerCount)
        assertEquals(1, textView.removedListenerCount)
    }

    private class SynchronousCallbackTextView : TextView(RuntimeEnvironment.getApplication()) {

        override fun addTextChangedListener(watcher: TextWatcher?) {
            super.addTextChangedListener(watcher)
            watcher?.onTextChanged("callback", 0, 0, "callback".length)
        }
    }

    private class TrackingTextView : TextView(RuntimeEnvironment.getApplication()) {

        var addedListenerCount = 0
            private set
        var removedListenerCount = 0
            private set

        override fun addTextChangedListener(watcher: TextWatcher?) {
            super.addTextChangedListener(watcher)
            addedListenerCount++
        }

        override fun removeTextChangedListener(watcher: TextWatcher?) {
            super.removeTextChangedListener(watcher)
            removedListenerCount++
        }
    }
}

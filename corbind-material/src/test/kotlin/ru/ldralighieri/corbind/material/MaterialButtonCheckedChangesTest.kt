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

package ru.ldralighieri.corbind.material

import android.content.Context
import android.os.Build
import android.os.Looper
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class MaterialButtonCheckedChangesTest {

    private val context: Context
        get() = materialTestContext()

    @After
    fun tearDown() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `state is read when collection starts`() = runBlocking {
        materialButtonCheckedChangesFixture(materialTestContext()).assertStateAtCollection()
    }

    @Test
    fun `each collection reads a fresh state`() = runBlocking {
        materialButtonCheckedChangesFixture(materialTestContext()).assertFreshStatePerCollection()
    }

    @Test
    fun `callback after initial value is delivered`() = runBlocking {
        materialButtonCheckedChangesFixture(materialTestContext()).assertCallbackAfterInitialValue()
    }

    @Test
    fun `flow is cold and first value cleans up listener`() = runBlocking {
        val button = TrackingMaterialButton(context)
        val changes = button.checkedChanges()
        assertEquals(0, button.addedListenerCount)
        changes.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, button.addedListenerCount)
        assertEquals(1, button.removedListenerCount)
    }

    @Test
    fun `cancelling collector removes listener exactly once`() = runBlocking {
        val button = TrackingMaterialButton(context)
        button.checkedChanges().test {
            assertEquals(false, awaitItem())
            assertEquals(1, button.addedListenerCount)
            cancel()
            cancel()
            assertEquals(1, button.removedListenerCount)
        }
    }

    @Test
    fun `already cancelled collector does not register listener`() = runBlocking {
        val button = TrackingMaterialButton(context)
        val parent = SupervisorJob().apply { cancel() }
        val cancelledScope = CoroutineScope(parent + Dispatchers.Unconfined)
        // A cancelled parent must prevent the collection from starting at all.
        val collection = cancelledScope.launch {
            button.checkedChanges().test { awaitItem() }
        }
        withTimeout(TIMEOUT_MILLIS) { collection.join() }
        assertTrue(collection.isCancelled)
        assertEquals(0, button.addedListenerCount)
        assertEquals(0, button.removedListenerCount)
    }

    @Test
    fun `two collectors are removed independently`() = runBlocking {
        val button = TrackingMaterialButton(context)
        val changes = button.checkedChanges()
        turbineScope {
            val first = changes.testIn(this)
            val second = changes.testIn(this)
            try {
                assertEquals(false, first.awaitItem())
                assertEquals(false, second.awaitItem())
                assertEquals(2, button.addedListenerCount)
                button.isChecked = true
                assertEquals(true, first.awaitItem())
                assertEquals(true, second.awaitItem())
                first.cancelAndIgnoreRemainingEvents()
                button.isChecked = false
                assertEquals(false, second.awaitItem())
                assertEquals(1, button.removedListenerCount)
                second.cancelAndIgnoreRemainingEvents()
                assertEquals(2, button.removedListenerCount)
            } finally {
                first.cancelAndIgnoreRemainingEvents()
                second.cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `collector cleanup preserves user checked listener`() = runBlocking {
        val button = TrackingMaterialButton(context)
        val userValues = mutableListOf<Boolean>()
        button.addOnCheckedChangeListener { _, checked -> userValues += checked }
        button.checkedChanges().test {
            assertEquals(false, awaitItem())
            assertEquals(2, button.addedListenerCount)
            cancelAndIgnoreRemainingEvents()
            button.isChecked = true
            assertEquals(listOf(true), userValues)
            assertEquals(1, button.removedListenerCount)
        }
    }

    @Test
    fun `callback during registration follows initial snapshot`() = runBlocking {
        val button = CallbackOnRegistrationButton(context)
        val changes = button.checkedChanges()
        assertEquals(0, button.registrationCount)
        changes.test {
            assertEquals(false, awaitItem())
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, button.registrationCount)
        assertEquals(1, button.cleanupCount)
    }

    @Test
    fun `drop initial value retains callback during registration`() = runBlocking {
        val button = CallbackOnRegistrationButton(context)
        button.checkedChanges().dropInitialValue().test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, button.registrationCount)
        assertEquals(1, button.cleanupCount)
    }

    @Test
    fun `collecting only initial value cleans up registration`() = runBlocking {
        val button = CallbackOnRegistrationButton(context)
        button.checkedChanges().test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, button.registrationCount)
        assertEquals(1, button.cleanupCount)
    }

    @Test
    fun `checked changes queues burst in FIFO order while collector is suspended`() = runBlocking {
        val button = MaterialButton(context).apply { isCheckable = true }
        val expected = buildList {
            add(false)
            repeat(BURST_SIZE) { index -> add(index % 2 == 0) }
        }
        val firstValueReceived = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        button.checkedChanges().buffer(capacity = 1)
            .onEach {
                if (!firstValueReceived.isCompleted) {
                    firstValueReceived.complete(Unit)
                    releaseCollector.await()
                }
            }
            .test {
                withTimeout(TIMEOUT_MILLIS) { firstValueReceived.await() }
                repeat(BURST_SIZE) { index -> button.isChecked = index % 2 == 0 }
                releaseCollector.complete(Unit)
                expected.forEach { assertEquals(it, awaitItem()) }
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `conflate explicitly keeps latest checked value for slow collector`() = runBlocking {
        val button = MaterialButton(context).apply { isCheckable = true }
        val firstValueReceived = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val upstreamCount = AtomicInteger()
        button.checkedChanges()
            .onEach { upstreamCount.incrementAndGet() }
            .conflate()
            .onEach {
                if (!firstValueReceived.isCompleted) {
                    firstValueReceived.complete(Unit)
                    releaseCollector.await()
                }
            }
            .take(2)
            .test {
                withTimeout(TIMEOUT_MILLIS) { firstValueReceived.await() }
                repeat(CONFLATED_BURST_SIZE) { index -> button.isChecked = index % 2 == 0 }
                withTimeout(TIMEOUT_MILLIS) {
                    while (upstreamCount.get() < CONFLATED_BURST_SIZE + 1) yield()
                }
                releaseCollector.complete(Unit)
                assertEquals(false, awaitItem())
                assertEquals(true, awaitItem())
                awaitComplete()
            }
    }

    private class TrackingMaterialButton(context: Context) : MaterialButton(context) {
        var addedListenerCount = 0
            private set
        var removedListenerCount = 0
            private set

        init {
            isCheckable = true
        }

        override fun addOnCheckedChangeListener(listener: OnCheckedChangeListener) {
            addedListenerCount++
            super.addOnCheckedChangeListener(listener)
        }

        override fun removeOnCheckedChangeListener(listener: OnCheckedChangeListener) {
            removedListenerCount++
            super.removeOnCheckedChangeListener(listener)
        }
    }

    private class CallbackOnRegistrationButton(context: Context) : MaterialButton(context) {
        var registrationCount = 0
            private set
        var cleanupCount = 0
            private set

        init {
            isCheckable = true
        }

        override fun addOnCheckedChangeListener(listener: OnCheckedChangeListener) {
            super.addOnCheckedChangeListener(listener)
            registrationCount++
            listener.onCheckedChanged(this, true)
        }

        override fun removeOnCheckedChangeListener(listener: OnCheckedChangeListener) {
            super.removeOnCheckedChangeListener(listener)
            cleanupCount++
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val BURST_SIZE = 128
        const val CONFLATED_BURST_SIZE = 127
    }
}

private fun materialButtonCheckedChangesFixture(context: Context): PerFileInitialValueFlowFixture {
    val button = MaterialButton(context).apply {
        isCheckable = true
        isChecked = false
    }
    return PerFileFixture(button.checkedChanges(), listOf(false, true, false), { button.isChecked = it })
}

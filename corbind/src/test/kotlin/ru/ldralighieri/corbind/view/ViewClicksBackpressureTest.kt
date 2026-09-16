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

package ru.ldralighieri.corbind.view

import android.os.Build
import android.os.Looper
import android.view.View
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class ViewClicksBackpressureTest {

    private lateinit var view: View
    private lateinit var scope: CoroutineScope
    private var binding: ReceiveChannel<*>? = null

    @Before
    fun setUp() {
        view = View(RuntimeEnvironment.getApplication())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        binding?.cancel()
        scope.cancel()
        idleMainLooper()
    }

    @Test
    fun `receive channel queues click burst before receiver`() = runBlocking {
        // given
        val clicks = view.clicks(scope)
        binding = clicks
        idleMainLooper()

        // when
        repeat(BURST_SIZE) { view.performClick() }

        // then
        val received = withTimeout(TIMEOUT_MILLIS) {
            List(BURST_SIZE) { clicks.receive() }
        }
        assertEquals(List(BURST_SIZE) { Unit }, received)
    }

    @Test
    fun `actor queues click burst while action is suspended`() = runBlocking {
        // given
        val firstActionStarted = CompletableDeferred<Unit>()
        val releaseAction = CompletableDeferred<Unit>()
        val allActionsCompleted = CompletableDeferred<Unit>()
        val actionCount = AtomicInteger()
        view.clicks(scope) {
            firstActionStarted.complete(Unit)
            releaseAction.await()
            if (actionCount.incrementAndGet() == BURST_SIZE) {
                allActionsCompleted.complete(Unit)
            }
        }
        idleMainLooper()

        view.performClick()
        idleMainLooper()
        withTimeout(TIMEOUT_MILLIS) { firstActionStarted.await() }

        // when
        repeat(BURST_SIZE - 1) { view.performClick() }
        releaseAction.complete(Unit)
        idleMainLooper()

        // then
        withTimeout(TIMEOUT_MILLIS) { allActionsCompleted.await() }
        assertEquals(BURST_SIZE, actionCount.get())
    }

    @Test
    fun `flow queues click burst while collector is suspended`() = runBlocking {
        // given
        val firstValueReceived = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()
        val allValuesReceived = CompletableDeferred<Unit>()
        val receivedCount = AtomicInteger()
        val collection = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            view.clicks().buffer(capacity = 1).collect {
                firstValueReceived.complete(Unit)
                releaseCollector.await()
                if (receivedCount.incrementAndGet() == BURST_SIZE) {
                    allValuesReceived.complete(Unit)
                }
            }
        }

        view.performClick()
        withTimeout(TIMEOUT_MILLIS) { firstValueReceived.await() }

        // when
        repeat(BURST_SIZE - 1) { view.performClick() }
        releaseCollector.complete(Unit)

        // then
        withTimeout(TIMEOUT_MILLIS) { allValuesReceived.await() }
        assertEquals(BURST_SIZE, receivedCount.get())
        collection.cancel()
    }

    @Test
    fun `long click handles queued rendezvous event`() = runBlocking {
        // given
        val longClicks = view.longClicks(scope)
        binding = longClicks
        idleMainLooper()

        // when
        val handled = view.performLongClick()

        // then
        assertTrue(handled)
        assertEquals(Unit, withTimeout(TIMEOUT_MILLIS) { longClicks.receive() })
    }

    @Test
    fun `long click rejects event after channel cancellation before listener cleanup`() {
        // given
        val longClicks = view.longClicks(scope)
        binding = longClicks
        idleMainLooper()
        assertNotNull(shadowOf(view).onLongClickListener)

        // when
        runOnBackground { longClicks.cancel() }

        // then
        assertTrue(scope.isActive)
        assertNotNull(shadowOf(view).onLongClickListener)
        assertFalse(requireNotNull(shadowOf(view).onLongClickListener).onLongClick(view))
        assertTrue(scope.isActive)
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun <T> runOnBackground(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        val thread = Thread { result.set(runCatching(block)) }
        thread.start()
        thread.join()
        return result.get().getOrThrow()
    }

    private companion object {

        const val BURST_SIZE = 128
        const val TIMEOUT_MILLIS = 5_000L
    }
}

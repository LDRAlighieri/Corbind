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

package ru.ldralighieri.corbind.internal

import android.os.Build
import android.os.Looper
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import ru.ldralighieri.corbind.view.clicks
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class CorbindReceiveChannelTest {

    private lateinit var scope: CoroutineScope
    private var binding: ReceiveChannel<*>? = null

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        binding?.cancel()
        scope.cancel()
        idleMainLooper()
    }

    @Test
    fun `active scope registers binding on main thread`() {
        // given
        val registrations = AtomicInteger()
        val registrationLooper = AtomicReference<Looper>()
        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                registrations.incrementAndGet()
                registrationLooper.set(Looper.myLooper())
                awaitClose()
            }
        }

        // when
        assertEquals(0, registrations.get())
        idleMainLooper()

        // then
        assertEquals(1, registrations.get())
        assertSame(Looper.getMainLooper(), registrationLooper.get())
        assertFalse(binding.requireChannel().tryReceive().isClosed)
    }

    @Test
    fun `already cancelled scope does not register binding`() {
        // given
        val registrations = AtomicInteger()
        val cleanups = AtomicInteger()
        scope.cancel()

        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                registrations.incrementAndGet()
                awaitClose { cleanups.incrementAndGet() }
            }
        }

        idleMainLooper()

        // then
        assertEquals(0, registrations.get())
        assertEquals(0, cleanups.get())
        assertTrue(binding.requireChannel().tryReceive().isClosed)
    }

    @Test
    fun `scope cancellation before main dispatch does not register binding`() {
        // given
        val registrations = AtomicInteger()
        val cleanups = AtomicInteger()
        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                registrations.incrementAndGet()
                awaitClose { cleanups.incrementAndGet() }
            }
        }

        // when
        runOnBackground { scope.cancel() }
        idleMainLooper()

        // then
        assertEquals(0, registrations.get())
        assertEquals(0, cleanups.get())
        assertTrue(binding.requireChannel().tryReceive().isClosed)
    }

    @Test
    fun `channel cancellation before main dispatch does not register binding`() {
        // given
        val registrations = AtomicInteger()
        val cleanups = AtomicInteger()
        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                registrations.incrementAndGet()
                awaitClose { cleanups.incrementAndGet() }
            }
        }

        // when
        runOnBackground { binding.requireChannel().cancel() }
        assertTrue(binding.requireChannel().tryReceive().isClosed)
        idleMainLooper()

        // then
        assertEquals(0, registrations.get())
        assertEquals(0, cleanups.get())
    }

    @Test
    fun `scope cancellation cleans up once on main thread`() {
        // given
        val cleanups = AtomicInteger()
        val cleanupLooper = AtomicReference<Looper>()
        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                awaitClose {
                    cleanups.incrementAndGet()
                    cleanupLooper.set(Looper.myLooper())
                }
            }
        }

        idleMainLooper()

        // when
        runOnBackground { scope.cancel() }
        idleMainLooper()

        binding.requireChannel().cancel()
        idleMainLooper()

        // then
        assertEquals(1, cleanups.get())
        assertSame(Looper.getMainLooper(), cleanupLooper.get())
        assertTrue(binding.requireChannel().tryReceive().isClosed)
    }

    @Test
    fun `scope cancellation closes channel while sibling is still cancelling`() {
        // given
        val parentJob = SupervisorJob()
        val siblingStarted = CountDownLatch(1)
        val siblingCancelling = CountDownLatch(1)
        val releaseSibling = CountDownLatch(1)
        val siblingFinished = CountDownLatch(1)
        val cleanups = AtomicInteger()
        scope = CoroutineScope(parentJob + Dispatchers.Unconfined)
        scope.launch(Dispatchers.Default) {
            siblingStarted.countDown()
            try {
                awaitCancellation()
            } finally {
                try {
                    withContext(NonCancellable) {
                        siblingCancelling.countDown()
                        releaseSibling.await()
                    }
                } finally {
                    siblingFinished.countDown()
                }
            }
        }

        assertTrue(siblingStarted.await(5, TimeUnit.SECONDS))

        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                awaitClose { cleanups.incrementAndGet() }
            }
        }

        idleMainLooper()

        try {
            // when
            runOnBackground { scope.cancel() }
            assertTrue(siblingCancelling.await(5, TimeUnit.SECONDS))
            idleMainLooper()

            // then
            assertFalse(parentJob.isCompleted)
            assertEquals(1, cleanups.get())
            assertTrue(binding.requireChannel().tryReceive().isClosed)
        } finally {
            releaseSibling.countDown()
            assertTrue(siblingFinished.await(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `normal scope job completion closes channel and cleans up on main thread`() {
        // given
        val parentJob = SupervisorJob()
        val cleanups = AtomicInteger()
        val cleanupLooper = AtomicReference<Looper>()
        scope = CoroutineScope(parentJob + Dispatchers.Unconfined)
        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                awaitClose {
                    cleanups.incrementAndGet()
                    cleanupLooper.set(Looper.myLooper())
                }
            }
        }

        idleMainLooper()

        // when
        assertTrue(runOnBackground { parentJob.complete() })
        idleMainLooper()

        // then
        assertTrue(parentJob.isCompleted)
        assertEquals(1, cleanups.get())
        assertSame(Looper.getMainLooper(), cleanupLooper.get())
        assertTrue(binding.requireChannel().tryReceive().isClosed)
    }

    @Test
    fun `channel cancellation cleans up on main without cancelling scope`() {
        // given
        val cleanups = AtomicInteger()
        val cleanupLooper = AtomicReference<Looper>()
        binding = runOnBackground {
            corbindReceiveChannel<Unit>(scope, Channel.UNLIMITED) {
                awaitClose {
                    cleanups.incrementAndGet()
                    cleanupLooper.set(Looper.myLooper())
                }
            }
        }

        idleMainLooper()

        // when
        runOnBackground { binding.requireChannel().cancel() }
        assertTrue(binding.requireChannel().tryReceive().isClosed)
        assertEquals(0, cleanups.get())
        idleMainLooper()

        // then
        assertEquals(1, cleanups.get())
        assertSame(Looper.getMainLooper(), cleanupLooper.get())
        assertTrue(scope.coroutineContext[Job]?.isActive == true)
    }

    @Test
    fun `view click listener is removed when scope is cancelled`() {
        // given
        val view = View(RuntimeEnvironment.getApplication())
        binding = view.clicks(scope, Channel.UNLIMITED)
        idleMainLooper()
        assertTrue(view.hasOnClickListeners())
        view.performClick()
        assertEquals(Unit, binding.requireChannel().tryReceive().getOrThrow())

        // when
        runOnBackground { scope.cancel() }
        idleMainLooper()

        // then
        assertFalse(view.hasOnClickListeners())
        assertTrue(binding.requireChannel().tryReceive().isClosed)
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun ReceiveChannel<*>?.requireChannel(): ReceiveChannel<*> = requireNotNull(this)

    private fun <T> runOnBackground(block: () -> T): T {
        val result = AtomicReference<Result<T>>()
        val thread = Thread { result.set(runCatching(block)) }
        thread.start()
        thread.join()
        return result.get().getOrThrow()
    }
}

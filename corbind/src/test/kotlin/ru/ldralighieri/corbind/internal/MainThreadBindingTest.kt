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

import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class MainThreadBindingTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        scope.cancel()
        idleMainLooper()
    }

    @Test
    fun `background flow collection registers and cleans up on main thread`() {
        // given
        val view = TrackingView(RuntimeEnvironment.getApplication())
        val flow = view.clicks().flowOn(Dispatchers.IO)

        // when
        val collection = runOnBackground {
            scope.launch(start = CoroutineStart.UNDISPATCHED) { flow.collect() }
        }
        idleMainLooper()

        // then
        assertSame(Looper.getMainLooper(), view.registrationLooper.get())

        // when
        runOnBackground { collection.cancel() }
        idleMainLooper()

        // then
        assertSame(Looper.getMainLooper(), view.cleanupLooper.get())
    }

    @Test
    fun `immediate cancellation before main dispatch does not register flow`() {
        // given
        val view = TrackingView(RuntimeEnvironment.getApplication())

        // when
        val collection = runOnBackground {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                view.clicks().collect()
            }.also(Job::cancel)
        }
        assertTrue(collection.isCancelled)
        idleMainLooper()

        // then
        assertEquals(0, view.registrations.get())
        assertEquals(0, view.cleanups.get())
    }

    @Test
    fun `action binding rejects background registration`() {
        // given
        val view = View(RuntimeEnvironment.getApplication())

        // when
        val failure = runOnBackground {
            runCatching { view.clicks(scope, Channel.UNLIMITED) {} }.exceptionOrNull()
        }

        // then
        assertTrue(failure is IllegalStateException)
        assertFalse(view.hasOnClickListeners())
    }

    @Test
    fun `action binding does not register for cancelled scope`() {
        // given
        val view = TrackingView(RuntimeEnvironment.getApplication())
        scope.cancel()

        // when
        view.clicks(scope, Channel.UNLIMITED) {}

        // then
        assertEquals(0, view.registrations.get())
        assertEquals(0, view.cleanups.get())
    }

    @Test
    fun `action binding cleanup dispatched from background runs on main thread`() {
        // given
        val view = TrackingView(RuntimeEnvironment.getApplication())
        view.clicks(scope, Channel.UNLIMITED) {}
        assertSame(Looper.getMainLooper(), view.registrationLooper.get())

        // when
        runOnBackground { scope.cancel() }
        idleMainLooper()

        // then
        assertSame(Looper.getMainLooper(), view.cleanupLooper.get())
        assertFalse(view.hasOnClickListeners())
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

    private class TrackingView(context: Context) : View(context) {

        val registrations = AtomicInteger()
        val cleanups = AtomicInteger()
        val registrationLooper = AtomicReference<Looper>()
        val cleanupLooper = AtomicReference<Looper>()

        override fun setOnClickListener(listener: OnClickListener?) {
            if (listener == null) {
                cleanups.incrementAndGet()
                cleanupLooper.set(Looper.myLooper())
            } else {
                registrations.incrementAndGet()
                registrationLooper.set(Looper.myLooper())
            }
            super.setOnClickListener(listener)
        }
    }
}

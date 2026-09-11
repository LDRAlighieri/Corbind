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

package ru.ldralighieri.corbind.activity

import android.os.Build
import android.os.Looper
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class OnBackPressedDispatcherBackProgressedTest {

    private lateinit var dispatcher: OnBackPressedDispatcher
    private lateinit var lifecycleOwner: TestLifecycleOwner
    private lateinit var scope: CoroutineScope
    private var binding: ReceiveChannel<Float>? = null

    @Before
    fun setUp() {
        dispatcher = OnBackPressedDispatcher()
        lifecycleOwner = TestLifecycleOwner()
        lifecycleOwner.registry.currentState = Lifecycle.State.STARTED
        scope = CoroutineScope(SupervisorJob())
    }

    @After
    fun tearDown() {
        binding?.cancel()
        scope.cancel()
    }

    @Test
    fun `committed predictive back is delegated to the next callback`() {
        // given
        var backPresses = 0
        dispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPresses++
                }
            },
        )
        bindBackProgressed()

        // when
        dispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
        dispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5f))
        val emittedProgress = binding?.tryReceive()?.getOrThrow()
        dispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idle()

        // then
        assertEquals(0.5f, emittedProgress)
        assertEquals(1, backPresses)
    }

    @Test
    fun `committed predictive back is delegated to the dispatcher fallback`() {
        // given
        var fallbackCalls = 0
        dispatcher = OnBackPressedDispatcher { fallbackCalls++ }
        bindBackProgressed()

        // when
        dispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
        dispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5f))
        dispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idle()

        // then
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun `cancelled predictive back is not delegated`() {
        // given
        var backPresses = 0
        dispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPresses++
                }
            },
        )
        bindBackProgressed()

        // when
        dispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
        dispatcher.dispatchOnBackProgressed(backEvent(progress = 0.5f))
        dispatcher.dispatchOnBackCancelled()
        shadowOf(Looper.getMainLooper()).idle()

        // then
        assertEquals(0, backPresses)

        // when
        dispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idle()

        // then
        assertEquals(1, backPresses)
    }

    @Test
    fun `stopped lifecycle does not intercept committed back`() {
        // given
        var backPresses = 0
        dispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPresses++
                }
            },
        )
        bindBackProgressed()
        lifecycleOwner.registry.currentState = Lifecycle.State.CREATED

        // when
        dispatcher.onBackPressed()
        shadowOf(Looper.getMainLooper()).idle()

        // then
        assertEquals(1, backPresses)
    }

    private fun bindBackProgressed() {
        binding = dispatcher.backProgressed(
            scope = scope,
            lifecycleOwner = lifecycleOwner,
            capacity = Channel.UNLIMITED,
        )
    }

    private fun backEvent(progress: Float): BackEventCompat = BackEventCompat(
        touchX = 0f,
        touchY = 0f,
        progress = progress,
        swipeEdge = BackEventCompat.EDGE_LEFT,
    )

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry
    }
}

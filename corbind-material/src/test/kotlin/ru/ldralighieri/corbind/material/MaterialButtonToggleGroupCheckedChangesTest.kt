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

import android.os.Build
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class MaterialButtonToggleGroupCheckedChangesTest {

    private lateinit var group: MaterialButtonToggleGroup
    private lateinit var scope: CoroutineScope
    private var binding: ReceiveChannel<Int>? = null
    private var firstButtonId = View.NO_ID
    private var secondButtonId = View.NO_ID

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar,
        )
        group = MaterialButtonToggleGroup(context).apply {
            isSingleSelection = true
        }
        firstButtonId = View.generateViewId()
        secondButtonId = View.generateViewId()
        group.addView(MaterialButton(context).apply { id = firstButtonId })
        group.addView(MaterialButton(context).apply { id = secondButtonId })
    }

    @After
    fun tearDown() {
        binding?.cancel()
        scope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `action emits clear after initial selection`() {
        // given
        val emissions = mutableListOf<Int>()
        group.check(firstButtonId)
        group.buttonCheckedChanges(scope, Channel.UNLIMITED) { emissions += it }
        shadowOf(Looper.getMainLooper()).idle()

        // when
        group.clearChecked()
        shadowOf(Looper.getMainLooper()).idle()

        // then
        assertEquals(listOf(firstButtonId, View.NO_ID), emissions)
    }

    @Test
    fun `channel emits clear after initial selection`() {
        // given
        group.check(firstButtonId)
        bindChannel()
        assertNext(firstButtonId)

        // when
        group.clearChecked()

        // then
        assertNext(View.NO_ID)
        assertNoEmission()
    }

    @Test
    fun `channel emits selection replacement`() {
        // given
        group.check(firstButtonId)
        bindChannel()
        assertNext(firstButtonId)

        // when
        group.check(secondButtonId)

        // then
        assertNext(View.NO_ID)
        assertNext(secondButtonId)
        assertNoEmission()
    }

    @Test
    fun `channel emits selection from empty state`() {
        // given
        bindChannel()
        assertNext(View.NO_ID)

        // when
        group.check(firstButtonId)

        // then
        assertNext(firstButtonId)
        assertNoEmission()
    }

    @Test
    fun `flow emits clear after initial selection`() {
        // given
        val emissions = Channel<Int>(Channel.UNLIMITED)
        group.check(firstButtonId)
        val collection = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            group.buttonCheckedChanges().collect { emissions.trySend(it) }
        }
        assertEquals(firstButtonId, emissions.tryReceive().getOrThrow())

        // when
        group.clearChecked()

        // then
        assertEquals(View.NO_ID, emissions.tryReceive().getOrThrow())
        assertTrue(emissions.tryReceive().isFailure)
        collection.cancel()
    }

    private fun bindChannel() {
        binding = group.buttonCheckedChanges(scope, Channel.UNLIMITED)
    }

    private fun assertNext(expected: Int) {
        assertEquals(expected, binding?.tryReceive()?.getOrThrow())
    }

    private fun assertNoEmission() {
        assertTrue(binding?.tryReceive()?.isFailure == true)
    }
}

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
import android.view.View
import app.cash.turbine.test
import com.google.android.material.chip.Chip
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class ChipCloseIconClicksTest {
    @Test
    fun `flow is cold emits close icon clicks and clears only its listener`() = runBlocking {
        val chip = TrackingChip(materialTestContext())
        var ordinaryClicks = 0
        chip.setOnClickListener { ordinaryClicks++ }
        val flow = chip.closeIconClicks()
        assertEquals(0, chip.nonNullCloseIconListenerCount)

        lateinit var closeListener: View.OnClickListener
        flow.test {
            assertEquals(1, chip.nonNullCloseIconListenerCount)
            closeListener = checkNotNull(chip.closeListener)
            closeListener.onClick(chip)
            assertEquals(Unit, awaitItem())
            expectNoEvents()
        }
        assertEquals(1, chip.nullCloseIconListenerCount)
        closeListener.onClick(chip)
        assertTrue(chip.performClick())
        assertEquals(1, ordinaryClicks)
    }

    private class TrackingChip(context: Context) : Chip(context) {
        var nonNullCloseIconListenerCount = 0
            private set
        var nullCloseIconListenerCount = 0
            private set
        var closeListener: View.OnClickListener? = null
            private set

        override fun setOnCloseIconClickListener(listener: View.OnClickListener?) {
            if (listener == null) nullCloseIconListenerCount++ else nonNullCloseIconListenerCount++
            closeListener = listener
            super.setOnCloseIconClickListener(listener)
        }
    }
}

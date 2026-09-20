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
import android.view.View
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.sidesheet.SideSheetCallback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class SideSheetBehaviorSlidesTest {

    @Test
    fun `slide callback emits offset and cancellation removes callback`() = runBlocking {
        val behavior = SlideTrackingSideSheet()
        val sheet = View(materialTestContext()).apply { installBehavior(behavior) }
        val slides = sheet.sideSheetSlides()
        assertEquals(0, behavior.added)

        slides.assertEventAndCleanup(
            expected = 0.6f,
            isRegistered = { behavior.callback != null },
            fire = { requireNotNull(behavior.callback).onSlide(sheet, 0.6f) },
            isRemoved = { behavior.callback == null && behavior.added == 1 && behavior.removed == 1 },
        )
    }

    private class SlideTrackingSideSheet : SideSheetBehavior<View>() {

        var callback: SideSheetCallback? = null
            private set
        var added = 0
            private set
        var removed = 0
            private set

        override fun addCallback(callback: SideSheetCallback) {
            this.callback = callback
            added++
        }

        override fun removeCallback(callback: SideSheetCallback) {
            if (this.callback === callback) this.callback = null
            removed++
        }
    }
}

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
import app.cash.turbine.test
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class TextInputLayoutStartIconLongClicksTest {
    @Test
    fun `long click requires handled predicate and active collector`() = runBlocking {
        val layout = TextInputLayout(materialTestContext())
        val icon = layout.findViewById<View>(com.google.android.material.R.id.text_input_start_icon)
        var shouldHandle = false
        val flow = layout.startIconLongClicks(handled = { shouldHandle })
        assertNull(shadowOf(icon).onLongClickListener)

        lateinit var capturedListener: View.OnLongClickListener
        flow.test {
            capturedListener = checkNotNull(shadowOf(icon).onLongClickListener)

            assertFalse(capturedListener.onLongClick(icon))
            expectNoEvents()

            shouldHandle = true
            assertTrue(capturedListener.onLongClick(icon))
            assertEquals(Unit, awaitItem())
        }
        assertNull(shadowOf(icon).onLongClickListener)
        assertFalse(capturedListener.onLongClick(icon))
    }
}

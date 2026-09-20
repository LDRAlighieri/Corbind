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

import android.content.DialogInterface
import android.os.Build
import app.cash.turbine.test
import com.google.android.material.datepicker.MaterialDatePicker
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
class MaterialDatePickerCancelsTest {

    @Test
    fun `flow is cold emits callback and removes listener on cancellation`() = runBlocking {
        // given
        val picker: MaterialDatePicker<Long> = MaterialDatePicker.Builder.datePicker().build()
        val listeners = picker.listeners()
        val flow = picker.cancels()
        assertTrue(listeners.isEmpty())

        // when
        flow.test {
            assertEquals(1, listeners.size)
            val listener = listeners.single()
            listener.onCancel(null)

            // then
            assertEquals(Unit, awaitItem())
            expectNoEvents()
        }
        assertTrue(listeners.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun MaterialDatePicker<Long>.listeners(): Set<DialogInterface.OnCancelListener> {
        val field = javaClass.getDeclaredField("onCancelListeners")
        field.isAccessible = true
        return field.get(this) as Set<DialogInterface.OnCancelListener>
    }
}

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
import android.view.ContextThemeWrapper
import android.view.View
import app.cash.turbine.test
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class MaterialButtonToggleGroupCheckedChangeEventsTest {

    @Test
    fun `flow is cold emits checked state and removes listener on cancellation`() = runBlocking {
        // given
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar,
        )
        val group = MaterialButtonToggleGroup(context)
        val buttonId = View.generateViewId()
        group.addView(MaterialButton(context).apply { id = buttonId })
        val listeners = group.listeners()
        val flow = group.buttonCheckedChangeEvents()
        assertTrue(listeners.isEmpty())

        // when
        flow.test {
            assertEquals(1, listeners.size)
            group.check(buttonId)
            group.uncheck(buttonId)

            // then
            assertEquals(
                MaterialButtonCheckedChangeEvent(buttonId, true),
                awaitItem(),
            )
            assertEquals(
                MaterialButtonCheckedChangeEvent(buttonId, false),
                awaitItem(),
            )
            expectNoEvents()
        }
        assertTrue(listeners.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun MaterialButtonToggleGroup.listeners(): Set<MaterialButtonToggleGroup.OnButtonCheckedListener> {
        val field = javaClass.getDeclaredField("onButtonCheckedListeners")
        field.isAccessible = true
        return field.get(this) as Set<MaterialButtonToggleGroup.OnButtonCheckedListener>
    }
}

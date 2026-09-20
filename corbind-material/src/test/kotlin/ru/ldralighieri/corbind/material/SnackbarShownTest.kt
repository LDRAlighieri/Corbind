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
import android.widget.FrameLayout
import com.google.android.material.snackbar.Snackbar
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
class SnackbarShownTest {

    @Test
    fun `shown callback emits snackbar and cancellation removes callback`() = runBlocking {
        val parent = FrameLayout(materialTestContext()).apply { id = android.R.id.content }
        val snackbar = Snackbar.make(parent, "Message", Snackbar.LENGTH_INDEFINITE)
        val shown = snackbar.shown()
        assertEquals(0, snackbar.testCallbacks().size)

        shown.assertEventAndCleanup(
            expected = snackbar,
            isRegistered = { snackbar.testCallbacks().size == 1 },
            fire = { snackbar.testCallbacks().single().onShown(snackbar) },
            isRemoved = { snackbar.testCallbacks().isEmpty() },
        )
    }
}

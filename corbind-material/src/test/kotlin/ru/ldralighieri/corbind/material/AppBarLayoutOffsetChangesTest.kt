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
import com.google.android.material.appbar.AppBarLayout
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
class AppBarLayoutOffsetChangesTest {

    @Test
    fun `offset callback emits vertical offset and cancellation removes listener`() = runBlocking {
        val appBar = TrackingAppBarLayout(materialTestContext())
        val changes = appBar.offsetChanges()
        assertEquals(0, appBar.added)

        changes.assertEventAndCleanup(
            expected = -24,
            isRegistered = { appBar.offsetListener != null },
            fire = { requireNotNull(appBar.offsetListener).onOffsetChanged(appBar, -24) },
            isRemoved = { appBar.offsetListener == null && appBar.added == 1 && appBar.removed == 1 },
        )
    }

    private class TrackingAppBarLayout(context: Context) : AppBarLayout(context) {

        var offsetListener: OnOffsetChangedListener? = null
            private set
        var added = 0
            private set
        var removed = 0
            private set

        override fun addOnOffsetChangedListener(listener: OnOffsetChangedListener) {
            super.addOnOffsetChangedListener(listener)
            offsetListener = listener
            added++
        }

        override fun removeOnOffsetChangedListener(listener: OnOffsetChangedListener) {
            super.removeOnOffsetChangedListener(listener)
            if (offsetListener === listener) offsetListener = null
            removed++
        }
    }
}

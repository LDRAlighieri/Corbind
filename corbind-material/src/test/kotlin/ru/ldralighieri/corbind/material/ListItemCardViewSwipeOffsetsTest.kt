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
import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import ru.ldralighieri.corbind.material.views.TrackingListItemCardView

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class ListItemCardViewSwipeOffsetsTest {

    @Test
    fun `flow is cold emits swipe offset and removes callback`() = runBlocking {
        val card = TrackingListItemCardView(material3TestContext())
        val offsets = card.swipeOffsets()
        assertNull(card.callback)

        offsets.test {
            assertNotNull(card.callback)
            expectNoEvents()
            card.onSwipe(-24)
            assertEquals(-24, awaitItem())
            expectNoEvents()
        }

        assertNull(card.callback)
        assertEquals(1, card.removed)
    }
}

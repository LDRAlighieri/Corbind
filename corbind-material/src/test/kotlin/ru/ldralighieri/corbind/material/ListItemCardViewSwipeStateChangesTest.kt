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
import android.view.Gravity
import app.cash.turbine.test
import com.google.android.material.listitem.ListItemCardView
import com.google.android.material.listitem.ListItemRevealLayout
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
class ListItemCardViewSwipeStateChangesTest {

    @Test
    fun `flow emits state changes and removes callback`() = runBlocking {
        val card = TrackingListItemCardView(material3TestContext())
        val reveal = ListItemRevealLayout(card.context)
        val states = card.swipeStateChanges()
        assertNull(card.callback)

        states.test {
            assertNotNull(card.callback)
            expectNoEvents()
            card.onSwipe(18)
            expectNoEvents()
            card.onSwipeStateChanged(ListItemCardView.STATE_OPEN, reveal, Gravity.END)
            assertEquals(ListItemCardView.STATE_OPEN, awaitItem())
            expectNoEvents()
        }

        assertNull(card.callback)
        assertEquals(1, card.removed)
    }
}

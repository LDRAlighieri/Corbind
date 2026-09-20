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
import com.google.android.material.search.SearchBar
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class SearchBarNavigationClicksTest {
    @Test
    fun `flow is cold emits navigation click and clears listener`() {
        val bar = TrackingSearchBar(material3TestContext())
        assertCallbackFlowContract(
            flow = bar.navigationClicks(),
            isRegistered = { bar.listener != null },
            dispatch = { checkNotNull(bar.listener).onClick(bar) },
            expected = Unit,
            isCleaned = { bar.listener == null && bar.cleanupCount == 1 },
        )
    }

    private class TrackingSearchBar(context: Context) : SearchBar(context) {
        var listener: View.OnClickListener? = null
            private set
        var cleanupCount = 0
            private set

        override fun setNavigationOnClickListener(listener: View.OnClickListener?) {
            super.setNavigationOnClickListener(listener)
            this.listener = listener
            if (listener == null) cleanupCount++
        }
    }
}

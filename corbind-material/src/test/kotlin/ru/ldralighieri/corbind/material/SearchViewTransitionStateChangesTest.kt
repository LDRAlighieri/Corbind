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
import com.google.android.material.search.SearchView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import ru.ldralighieri.corbind.material.views.TrackingSearchView

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class SearchViewTransitionStateChangesTest {
    @Test
    fun `flow is cold emits transition and removes listener`() {
        val view = TrackingSearchView(material3TestContext())
        assertCallbackFlowContract(
            flow = view.transitionStateChanges(),
            isRegistered = { view.listener != null },
            dispatch = {
                checkNotNull(view.listener).onStateChanged(
                    view,
                    SearchView.TransitionState.HIDDEN,
                    SearchView.TransitionState.SHOWN,
                )
            },
            expected = SearchView.TransitionState.SHOWN,
            isCleaned = { view.listener == null && view.cleanupCount == 1 },
        )
    }
}

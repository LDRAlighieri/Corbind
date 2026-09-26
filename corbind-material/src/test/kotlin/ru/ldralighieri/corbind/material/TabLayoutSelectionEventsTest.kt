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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import ru.ldralighieri.corbind.material.views.TrackingTabLayout

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class TabLayoutSelectionEventsTest {
    private fun fixture() = TabSelectionEventsFixture(materialTestContext())

    @Test
    fun `flow is cold and reads state when collection starts`() {
        InitialEmittingFlowContract.coldAndReadsStateWhenCollected(fixture())
    }

    @Test
    fun `each collection reads a fresh state snapshot`() {
        InitialEmittingFlowContract.eachCollectionReadsFreshState(fixture())
    }

    @Test
    fun `callback after initial value is delivered`() {
        InitialEmittingFlowContract.callbackFollowsInitialValue(fixture())
    }

    @Test
    fun `callback during registration follows initial value`() {
        InitialEmittingFlowContract.registrationCallbackFollowsInitialValue(fixture())
    }

    @Test
    fun `collecting only initial value cleans up listener`() {
        InitialEmittingFlowContract.initialValueOnlyCleansUp(fixture())
    }
}

private class TabSelectionEventsFixture(context: Context) : InitialEmittingFlowFixture {
    private val view = TrackingTabLayout(context)

    override val registrationCount: Int get() = view.registrationCount
    override val cleanupCount: Int get() = view.cleanupCount
    override val isListenerRegistered: Boolean get() = view.tabSelectedListener != null

    override fun flow(): Flow<String> = view.selectionEvents().map { event ->
        val name = when (event) {
            is TabLayoutSelectionSelectedEvent -> "selected"
            is TabLayoutSelectionReselectedEvent -> "reselected"
            is TabLayoutSelectionUnselectedEvent -> "unselected"
        }
        "$name:${event.tab.tag}"
    }
    override fun select(itemId: Int) {
        view.selectTab(view.tab(itemId))
    }
    override fun dispatchSelection(itemId: Int) {
        checkNotNull(view.tabSelectedListener).onTabSelected(view.tab(itemId))
    }
    override fun dispatchSelectionDuringNextRegistration(itemId: Int) {
        view.dispatchSelectionDuringNextRegistration(itemId)
    }
}

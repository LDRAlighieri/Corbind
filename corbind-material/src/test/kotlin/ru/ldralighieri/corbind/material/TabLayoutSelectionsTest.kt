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
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class TabLayoutSelectionsTest {
    private fun fixture() = TabSelectionsFixture(materialTestContext())

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

private class TabSelectionsFixture(context: Context) : InitialEmittingFlowFixture {
    private val view = TrackingSelectionsTabLayout(context)

    override val registrationCount: Int get() = view.registrationCount
    override val cleanupCount: Int get() = view.cleanupCount
    override val isListenerRegistered: Boolean get() = view.tabSelectedListener != null

    override fun flow(): Flow<String> = view.selections().map { "selected:${it.tag}" }
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

private class TrackingSelectionsTabLayout(context: Context) : TabLayout(context) {
    var registrationCount = 0
        private set
    var cleanupCount = 0
        private set
    var tabSelectedListener: OnTabSelectedListener? = null
        private set
    private var selectionDuringRegistration: Int? = null

    init {
        addTab(newTab().apply { tag = FIRST_MATERIAL_ITEM })
        addTab(newTab().apply { tag = SECOND_MATERIAL_ITEM })
        selectTab(tab(FIRST_MATERIAL_ITEM))
        resetTracking()
    }

    fun tab(itemId: Int): Tab = (0 until tabCount).mapNotNull(::getTabAt).first { it.tag == itemId }

    override fun addOnTabSelectedListener(listener: OnTabSelectedListener) {
        super.addOnTabSelectedListener(listener)
        registrationCount++
        tabSelectedListener = listener
        selectionDuringRegistration?.let { itemId ->
            selectionDuringRegistration = null
            listener.onTabSelected(tab(itemId))
        }
    }

    override fun removeOnTabSelectedListener(listener: OnTabSelectedListener) {
        super.removeOnTabSelectedListener(listener)
        cleanupCount++
        if (tabSelectedListener === listener) tabSelectedListener = null
    }

    fun dispatchSelectionDuringNextRegistration(itemId: Int) {
        selectionDuringRegistration = itemId
    }
    fun resetTracking() {
        registrationCount = 0
        cleanupCount = 0
        tabSelectedListener = null
    }
}

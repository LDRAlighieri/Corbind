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

package ru.ldralighieri.corbind.material.views

import android.content.Context
import com.google.android.material.tabs.TabLayout
import ru.ldralighieri.corbind.material.FIRST_MATERIAL_ITEM
import ru.ldralighieri.corbind.material.SECOND_MATERIAL_ITEM

internal class TrackingTabLayout(context: Context) : TabLayout(context) {
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

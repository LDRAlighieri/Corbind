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
import com.google.android.material.search.SearchView

internal class TrackingSearchView(context: Context) : SearchView(context) {
    var listener: TransitionListener? = null
        private set

    var cleanupCount = 0
        private set

    override fun addTransitionListener(listener: TransitionListener) {
        super.addTransitionListener(listener)
        this.listener = listener
    }

    override fun removeTransitionListener(listener: TransitionListener) {
        super.removeTransitionListener(listener)
        if (this.listener === listener) this.listener = null
        cleanupCount++
    }
}

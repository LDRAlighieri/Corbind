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

package ru.ldralighieri.corbind.widget

import android.os.Build
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
@LooperMode(LooperMode.Mode.PAUSED)
class InitialValueReceiveChannelTest(
    private val bindingCase: BindingCase,
) {

    private lateinit var scope: CoroutineScope
    private var binding: ReceiveChannel<*>? = null

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        binding?.cancel()
        scope.cancel()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `default rendezvous channel delivers initial value`() {
        // when
        val expectedAndChannel = bindingCase.bind(scope)
        binding = expectedAndChannel.channel

        // then
        val actual = binding?.tryReceive()?.getOrThrow().toComparableValue()
        assertEquals(expectedAndChannel.expected, actual)
    }

    private fun Any?.toComparableValue(): Any? = when (this) {
        is CharSequence -> toString()
        is TextViewTextChangeEvent -> text.toString()
        else -> this
    }

    enum class BindingCase {
        TEXT_CHANGES,
        TEXT_CHANGE_EVENTS,
        CHECKED_CHANGES,
        SEEK_BAR_CHANGES,
        SEEK_BAR_USER_CHANGES,
        SEEK_BAR_SYSTEM_CHANGES,
        ADAPTER_DATA_CHANGES,
        ;

        fun bind(scope: CoroutineScope): ExpectedAndChannel {
            val context = RuntimeEnvironment.getApplication()
            return when (this) {
                TEXT_CHANGES -> {
                    val view = TextView(context).apply { text = INITIAL_TEXT }
                    ExpectedAndChannel(INITIAL_TEXT, view.textChanges(scope))
                }

                TEXT_CHANGE_EVENTS -> {
                    val view = EditText(context).apply { setText(INITIAL_TEXT) }
                    ExpectedAndChannel(INITIAL_TEXT, view.textChangeEvents(scope))
                }

                CHECKED_CHANGES -> {
                    val button = CheckBox(context).apply { isChecked = true }
                    ExpectedAndChannel(true, button.checkedChanges(scope))
                }

                SEEK_BAR_CHANGES -> {
                    val seekBar = SeekBar(context).apply { progress = INITIAL_PROGRESS }
                    ExpectedAndChannel(INITIAL_PROGRESS, seekBar.changes(scope))
                }

                SEEK_BAR_USER_CHANGES -> {
                    val seekBar = SeekBar(context).apply { progress = INITIAL_PROGRESS }
                    ExpectedAndChannel(INITIAL_PROGRESS, seekBar.userChanges(scope))
                }

                SEEK_BAR_SYSTEM_CHANGES -> {
                    val seekBar = SeekBar(context).apply { progress = INITIAL_PROGRESS }
                    ExpectedAndChannel(INITIAL_PROGRESS, seekBar.systemChanges(scope))
                }

                ADAPTER_DATA_CHANGES -> {
                    val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, listOf("item"))
                    ExpectedAndChannel(adapter, adapter.dataChanges(scope))
                }
            }
        }
    }

    data class ExpectedAndChannel(
        val expected: Any,
        val channel: ReceiveChannel<*>,
    )

    companion object {

        private const val INITIAL_TEXT = "initial"
        private const val INITIAL_PROGRESS = 42

        @JvmStatic
        @Parameters(name = "{0}")
        fun parameters(): List<Array<BindingCase>> = BindingCase.entries.map { arrayOf(it) }
    }
}

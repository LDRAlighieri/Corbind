/*
 * Copyright 2019 Vladimir Raupov
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

package ru.ldralighieri.corbind.sample

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.ldralighieri.corbind.activity.OnBackPressed
import ru.ldralighieri.corbind.activity.OnBackProgressed
import ru.ldralighieri.corbind.activity.backEvents
import ru.ldralighieri.corbind.sample.core.extensions.hideSoftInput
import ru.ldralighieri.corbind.sample.core.extensions.toPx
import ru.ldralighieri.corbind.sample.databinding.ActivityLoginBinding
import ru.ldralighieri.corbind.swiperefreshlayout.refreshes
import ru.ldralighieri.corbind.view.clicks
import ru.ldralighieri.corbind.widget.editorActionEvents
import ru.ldralighieri.corbind.widget.textChanges

class LoginActivity : AppCompatActivity() {

    private companion object {
        const val TRANSITION_X_THRESHOLD_DP = 40
    }

    private lateinit var binding: ActivityLoginBinding

    private val transitionXThresholdPx: Float by lazy { TRANSITION_X_THRESHOLD_DP.toPx }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindViews()
    }

    private fun bindViews() {
        with(binding) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    combine(
                        etEmail.textChanges()
                            .map { Patterns.EMAIL_ADDRESS.matcher(it).matches() },
                        etPassword.textChanges()
                            .map { it.length > 7 },
                        transform = { email, password -> email && password }
                    )
                        .onEach { btLogin.isEnabled = it }
                        .launchIn(this)

                    merge(
                        btLogin.clicks(),
                        etPassword.editorActionEvents()
                            .filter { it.actionId == EditorInfo.IME_ACTION_DONE }
                            .filter { btLogin.isEnabled }
                            .onEach { hideSoftInput() }
                    )
                        .onEach {
                            Toast.makeText(
                                this@LoginActivity,
                                R.string.login_success_message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .launchIn(this)

                    swipe.refreshes()
                        .onEach {
                            etEmail.text?.clear()
                            etPassword.text?.clear()
                            swipe.isRefreshing = false
                            hideSoftInput()
                            Toast.makeText(
                                this@LoginActivity,
                                R.string.login_swipe_message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .launchIn(this)

                    onBackPressedDispatcher.backEvents(lifecycleOwner = this@LoginActivity)
                        .onEach { event ->
                            when {
                                event is OnBackPressed -> finish()
                                event is OnBackProgressed -> {
                                    with (event) {
                                        val direction: Int =
                                            if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1
                                            else -1
                                        
                                        tvTitle.translationX =
                                            direction * transitionXThresholdPx * backEvent.progress
                                    }
                                }
                            }
                        }
                        .launchIn(this)
                }
            }
        }
    }
}

/*
 * Copyright 2022 Vladimir Raupov
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

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

@Suppress("unused")
class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.dokka")

            tasks.withType<DokkaTask>().configureEach {
                dokkaSourceSets.named("main") {
                    jdkVersion.set(JavaVersion.VERSION_11.majorVersion.toInt())

                    skipDeprecated.set(false)
                    reportUndocumented.set(false)
                    skipEmptyPackages.set(true)

                    sourceLink {
                        val relPath = rootProject.projectDir.toPath().relativize(projectDir.toPath())
                        localDirectory.set(file("src/main/kotlin"))
                        remoteUrl.set(URL("https://github.com/LDRAlighieri/Corbind/tree/master/$relPath/src/main/kotlin"))
                        remoteLineSuffix.set("#L")
                    }

                    externalDocumentationLink {
                        url.set(URL("https://developer.android.com/reference/"))
                        packageListUrl.set(URL("https://developer.android.com/reference/package-list"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://developer.android.com/reference/"))
                        packageListUrl.set(URL("https://developer.android.com/reference/androidx/package-list"))
                    }

                    externalDocumentationLink {
                        url.set(URL("https://developer.android.com/reference/"))
                        packageListUrl.set(URL("https://developer.android.com/reference/com/google/android/material/package-list"))
                    }
                }
            }
        }
    }
}

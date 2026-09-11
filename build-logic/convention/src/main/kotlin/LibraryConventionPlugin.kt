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

import com.android.build.api.dsl.LibraryExtension
import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import ru.ldralighieri.corbind.configureKotlinAndroid

@Suppress("unused")
class LibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)

                buildTypes {
                    release {
                        isMinifyEnabled = false
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                }
            }

            configureApiValidation()
        }
    }

    private fun Project.configureApiValidation() {
        // BCV does not discover Android libraries that use AGP 9 built-in Kotlin.
        // Register its JVM API tasks against the release compilation until upstream support lands:
        // https://github.com/Kotlin/binary-compatibility-validator/issues/312
        afterEvaluate {
            val kotlinClasses = tasks.named("compileReleaseKotlin").map { it.outputs.files }
            val javaClasses = tasks.named("compileReleaseJavaWithJavac").map { it.outputs.files }
            val generatedApi = layout.buildDirectory.file("api/$name.api")
            val committedApi = layout.projectDirectory.file("api/$name.api")

            val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
                description = "Builds the public API dump for $name."
                inputClassesDirs.from(kotlinClasses, javaClasses)
                outputApiFile.set(generatedApi)
                runtimeClasspath.from(configurations.named("bcv-rt-jvm-cp-resolver"))
            }

            val apiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
                group = "verification"
                description = "Checks the public API of $name against the committed dump."
                projectApiFile.set(committedApi)
                generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
            }

            val apiDump = tasks.register<ApiDumpTask>("apiDump") {
                group = "verification"
                description = "Updates the committed public API dump for $name."
                generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
                committedApiFile.set(committedApi)
            }

            apiCheck.configure {
                mustRunAfter(apiDump)
            }

            tasks.named("check") {
                dependsOn(apiCheck)
            }
        }
    }
}

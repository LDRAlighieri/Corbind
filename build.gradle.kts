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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt

// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.detekt)
    alias(libs.plugins.gver)
}

// Binary compatibility validator
apiValidation {
    ignoredProjects.add("sample")
    ignoredPackages.add("ru/ldralighieri/corbind/internal")
}

// Detekt
detekt {
    allRules = false
    buildUponDefaultConfig = true

    config = files("default-detekt-config.yml")
    source = files(
            "corbind/src/main/kotlin",
            "corbind-activity/src/main/kotlin",
            "corbind-appcompat/src/main/kotlin",
            "corbind-core/src/main/kotlin",
            "corbind-drawerlayout/src/main/kotlin",
            "corbind-fragment/src/main/kotlin",
            "corbind-leanback/src/main/kotlin",
            "corbind-lifecycle/src/main/kotlin",
            "corbind-material/src/main/kotlin",
            "corbind-navigation/src/main/kotlin",
            "corbind-recyclerview/src/main/kotlin",
            "corbind-slidingpanelayout/src/main/kotlin",
            "corbind-swiperefreshlayout/src/main/kotlin",
            "corbind-viewpager/src/main/kotlin",
            "corbind-viewpager2/src/main/kotlin"
    )
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_17.toString()
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

// Dependency updates
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL").any { version.contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

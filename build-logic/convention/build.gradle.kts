import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

plugins {
    `kotlin-dsl`
}

group = "ru.ldralighieri.corbind.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.maven.publish.gradlePlugin)
    implementation(libs.dokka.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("dokka") {
            id = "corbind.dokka"
            implementationClass = "DokkaConventionPlugin"
        }

        register("library") {
            id = "corbind.library"
            implementationClass = "LibraryConventionPlugin"
        }

        register("mavenPublish") {
            id = "corbind.maven.publish"
            implementationClass = "MavenPublishConventionPlugin"
        }

        register("spotless") {
            id = "corbind.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}

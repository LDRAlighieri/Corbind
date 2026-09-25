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

plugins {
    id("com.android.application")
}

android {
    namespace = "ru.ldralighieri.corbind.sample"

    buildToolsVersion = providers.gradleProperty("buildTools").get()
    compileSdk = providers.gradleProperty("compileSdk").get().toInt()

    defaultConfig {
        applicationId = "ru.ldralighieri.corbind.example"
        minSdk = providers.gradleProperty("minSdk").get().toInt()
        targetSdk = providers.gradleProperty("targetSdk").get().toInt()
        versionCode = 1
        versionName = providers.gradleProperty("VERSION_NAME").get()

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        val debug = getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            // This demo APK uses the debug key for local and CI builds; it is not a production release.
            signingConfig = debug.signingConfig
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes.apply {
            add("META-INF/NOTICE")
            add("META-INF/NOTICE.txt")
            add("META-INF/notice.txt")
            add("META-INF/LICENSE")
            add("META-INF/LICENSE.txt")
            add("META-INF/license.txt")
            add("META-INF/atomicfu.kotlin_module")
        }
    }

    buildFeatures { viewBinding = true }

    lint {
        abortOnError = true
        warningsAsErrors = true
    }
}

dependencies {
    implementation(projects.corbindSwiperefreshlayout)
    implementation(projects.corbindActivity)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
}

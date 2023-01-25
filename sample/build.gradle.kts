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
    id("kotlin-android")
}

android {
    val compileSdk: String by project
    val minSdk: String by project
    val targetSdk: String by project
    @Suppress("LocalVariableName") val VERSION_NAME: String by project

    namespace = "ru.ldralighieri.corbind.sample"

    this.compileSdk = compileSdk.toInt()
    defaultConfig {
        applicationId = "ru.ldralighieri.corbind.example"
        this.minSdk = minSdk.toInt()
        this.targetSdk = targetSdk.toInt()
        versionCode = 1
        versionName = VERSION_NAME

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        val debug by getting {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = debug.signingConfig
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions {
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
}

dependencies {
    implementation(projects.corbindSwiperefreshlayout)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
}

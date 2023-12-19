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

@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// https://github.com/gradle/gradle/issues/16608
rootProject.name = "CorbindProject"

include(":corbind-bom")

include(":corbind")
include(":corbind-activity")
include(":corbind-appcompat")
include(":corbind-core")
include(":corbind-drawerlayout")
include(":corbind-fragment")
include(":corbind-leanback")
include(":corbind-lifecycle")
include(":corbind-material")
include(":corbind-navigation")
include(":corbind-recyclerview")
include(":corbind-slidingpanelayout")
include(":corbind-swiperefreshlayout")
include(":corbind-viewpager")
include(":corbind-viewpager2")

include(":sample")

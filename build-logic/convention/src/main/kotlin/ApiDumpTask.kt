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

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Based on the Android BCV Bridge implementation:
 * https://github.com/tjokinen/android-bcv-bridge/blob/main/src/main/kotlin/io/github/tjokinen/androidbcvbridge/ApiDumpTask.kt
 */
@DisableCachingByDefault(because = "Trivial copy of the generated dump over the committed file")
abstract class ApiDumpTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val generatedApiFile: RegularFileProperty

    @get:OutputFile
    abstract val committedApiFile: RegularFileProperty

    @TaskAction
    fun dump() {
        val generated = generatedApiFile.get().asFile
        val committed = committedApiFile.get().asFile
        committed.parentFile.mkdirs()
        generated.copyTo(committed, overwrite = true)
    }
}

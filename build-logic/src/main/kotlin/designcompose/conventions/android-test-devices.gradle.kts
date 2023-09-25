/*
 * Copyright 2023 Google LLC
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

package designcompose.conventions

apply<ATDPlugin>()

// AGP considers Tasks to be an implementation detail, so there's no accessors for them.
// Have to look them up the old fashioned way
project.plugins.withType(com.android.build.gradle.BasePlugin::class.java) {
    afterEvaluate {
        val atdApi30Task =
            tasks.named("gmdAtdApi30DebugAndroidTest").also {
                it.configure { group = "DesignCompose Developer" }
            }
        val gmdAllApisTask =
            tasks.named("gmdAllApisGroupDebugAndroidTest").also {
                it.configure { group = "DesignCompose Developer" }
            }
        tasks.register("gmdTestQuick") {
            group = "DesignCompose Developer"
            dependsOn(atdApi30Task)
        }
        tasks.register("gmdTestStandard") {
            group = "DesignCompose Developer"
            dependsOn(atdApi30Task, tasks.named("gmdApi34DebugAndroidTest"))
        }
        tasks.register("gmdTestAll") {
            group = "DesignCompose Developer"
            dependsOn(gmdAllApisTask)
        }
    }
}

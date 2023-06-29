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
        tasks.named("tabletAtdApi30DebugAndroidTest").configure {
            group = "DesignCompose Developer"
        }
        tasks.named("tabletAllApisGroupDebugAndroidTest").configure {
            group = "DesignCompose Developer"
        }
    }
}

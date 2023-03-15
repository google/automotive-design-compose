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

package com.android.designcompose

// A simple JNI test object. Used to ensure that the JNI is properly compiled and can be accessed by
// downstream consumers, like apps.
// TODO: I'd like this to be much more limited, and especially not published externally. But for
// the sake of expediency and keeping the change size down, I'm simply including it in
// DesignCompose.
object HelloJni {
    // This declares that the static `hello` method will be provided
    // a native library.
    external fun hello(input: String?): String

    init {
        // This actually loads the shared object that we'll be creating.
        // The actual location of the .so or .dll may differ based on your
        // platform.
        System.loadLibrary("live_update")
    }
}

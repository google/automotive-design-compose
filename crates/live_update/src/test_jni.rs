// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// extern crate jni;
//
// use jni::objects::{JClass, JString};
// use jni::sys::jstring;
// use jni::JNIEnv;
//
// #[cfg(debug_assertions)]
// #[no_mangle]
// pub extern "system" fn Java_com_android_designcompose_HelloJni_hello(
//     env: JNIEnv,
//     _class: JClass,
//     input: JString,
// ) -> jstring {
//     let input: String = env.get_string(input).expect("Couldn't get java string!").into();
//
//     let output =
//         env.new_string(format!("Hello, {}!", input)).expect("Couldn't create java string!");
//
//     output.into_raw()
// }

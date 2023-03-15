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

use std::ffi::c_void;

use jni::objects::{JClass, JString};
use jni::sys::{jbyteArray, jint, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM};

use crate::{fetch_doc, ConvertRequest, Error};

fn throw_basic_exception(env: JNIEnv, msg: String) {
    //An error occurring while trying to throw an exception shouldn't happen, but let's at least panic with a decent error message
    env.throw(msg)
        .expect("Error while trying to throw Exception");
}

fn jni_fetch_doc(
    env: JNIEnv,
    _class: JClass,
    jdoc_id: JString,
    jrequest_json: JString,
) -> jbyteArray {
    let doc_id: String = match env.get_string(jdoc_id) {
        Ok(it) => it.into(),
        Err(_) => {
            throw_basic_exception(env, format!("Internal JNI Error"));
            return std::ptr::null_mut();
        }
    };

    let request_json: String = match env.get_string(jrequest_json) {
        Ok(it) => it.into(),
        Err(_) => {
            throw_basic_exception(env, format!("Internal JNI Error"));
            return std::ptr::null_mut();
        }
    };

    let ser_result = match jni_fetch_doc_impl(doc_id, request_json) {
        Ok(it) => it,
        Err(err) => {
            // TODO: Throw more specific exceptions
            throw_basic_exception(env, format!("Failure to fetch doc: {:?}", err));
            return std::ptr::null_mut();
        }
    };

    env.byte_array_from_slice(&ser_result).unwrap_or_else(|_| {
        throw_basic_exception(env, format!("Internal JNI Error"));
        return std::ptr::null_mut();
    })
}

fn jni_fetch_doc_impl(doc_id: String, request_json: String) -> Result<Vec<u8>, Error> {
    let request: ConvertRequest = serde_json::from_str(&request_json)?;
    let conv_resp: crate::ConvertResponse = fetch_doc(&doc_id, request)?;

    bincode::serialize(&conv_resp).map_err(|e| e.into())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _: *mut c_void) -> jint {
    let env = vm.get_env().expect("Cannot get reference to the JNIEnv");

    let cls = env
        .find_class("com/android/designcompose/LiveUpdateJni")
        .expect("Unable to find com.android.designcompose.LiveUpdateJni class");

    if !env
        .register_native_methods(
            cls,
            &[jni::NativeMethod {
                name: "jniFetchDoc".into(),
                sig: "(Ljava/lang/String;Ljava/lang/String;)[B".into(),
                fn_ptr: jni_fetch_doc as *mut c_void,
            }],
        )
        .is_ok()
    {
        println!("Unable to register native methods");
    }
    JNI_VERSION_1_6
}

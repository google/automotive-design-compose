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

use android_logger::Config;
use jni::objects::{JByteArray, JObject};
use jni::objects::{JClass, JString};
use jni::sys::{jint, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM};
use log::error;
use log::LevelFilter;

use figma_import::{fetch_doc, ConvertRequest, ProxyConfig};

use crate::error_map::map_err_to_exception;

fn throw_basic_exception(env: &mut JNIEnv, msg: String) {
    // An error occurring while trying to throw an exception shouldn't happen,
    // but let's at least panic with a decent error message
    env.throw(msg).expect("Error while trying to throw Exception");
}

fn get_string(env: &mut JNIEnv, obj: &JObject) -> Option<String> {
    match env.get_string(obj.into()) {
        Ok(it) => Some(it.into()),
        Err(_) => {
            // obj is null or not a java.lang.String
            None
        }
    }
}

fn get_proxy_config(env: &mut JNIEnv, input: &JObject) -> Result<ProxyConfig, jni::errors::Error> {
    let http_proxy_config = env
        .get_field(input, "httpProxyConfig", "Lcom/android/designcompose/HttpProxyConfig;")?
        .l()?;
    let proxy_spec_field =
        env.get_field(http_proxy_config, "proxySpec", "Ljava/lang/String;")?.l()?;
    Ok(match get_string(env, &proxy_spec_field) {
        Some(x) => ProxyConfig::HttpProxyConfig(x),
        None => ProxyConfig::None,
    })
}

fn jni_fetch_doc<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    jdoc_id: JString,
    jrequest_json: JString,
    jproxy_config: JObject,
) -> JByteArray<'local> {
    let doc_id: String = match env.get_string(&jdoc_id) {
        Ok(it) => it.into(),
        Err(_) => {
            throw_basic_exception(&mut env, "Internal JNI Error".to_string());
            return JObject::null().into();
        }
    };

    let request_json: String = match env.get_string(&jrequest_json) {
        Ok(it) => it.into(),
        Err(_) => {
            throw_basic_exception(&mut env, "Internal JNI Error".to_string());
            return JObject::null().into();
        }
    };

    let proxy_config: ProxyConfig = match get_proxy_config(&mut env, &jproxy_config) {
        Ok(it) => it,
        Err(e) => {
            eprintln!("Error getting proxy_config - {e}");
            ProxyConfig::None
        }
    };

    let ser_result = match jni_fetch_doc_impl(&mut env, doc_id, request_json, &proxy_config) {
        Ok(it) => it,
        Err(_err) => {
            return JObject::null().into();
        }
    };

    env.byte_array_from_slice(&ser_result).unwrap_or_else(|_| {
        throw_basic_exception(&mut env, "Internal JNI Error".to_string());
        JObject::null().into()
    })
}

fn jni_fetch_doc_impl(
    env: &mut JNIEnv,
    doc_id: String,
    request_json: String,
    proxy_config: &ProxyConfig,
) -> Result<Vec<u8>, figma_import::Error> {
    let request: ConvertRequest = serde_json::from_str(&request_json)?;

    let convert_result: figma_import::ConvertResponse =
        match fetch_doc(&doc_id, request, proxy_config) {
            Ok(it) => it,
            Err(err) => {
                map_err_to_exception(env, &err, doc_id).expect("Failed to throw exception");
                return Err(err);
            }
        };

    bincode::serialize(&convert_result).map_err(|e| e.into())
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _: *mut c_void) -> jint {
    // Enable the logger, limit the log level to info to reduce spam
    android_logger::init_once(
        Config::default().with_tag("LiveUpdateJNI").with_max_level(LevelFilter::Info),
    );

    let mut env = vm.get_env().expect("Cannot get reference to the JNIEnv");

    let cls = env
        .find_class("com/android/designcompose/LiveUpdateJni")
        .expect("Unable to find com.android.designcompose.LiveUpdateJni class");

    if env
        .register_native_methods(
            cls,
            &[jni::NativeMethod {
                name: "jniFetchDoc".into(),
                sig: "(Ljava/lang/String;Ljava/lang/String;Lcom/android/designcompose/ProxyConfig;)[B".into(),
                fn_ptr: jni_fetch_doc as *mut c_void,
            }],
        )
        .is_err()
    {
        error!("Unable to register native methods");
    }
    JNI_VERSION_1_6
}

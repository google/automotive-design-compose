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

use crate::error_map::map_err_to_exception;
use android_logger::Config;
use figma_import::layout::LayoutChangedResponse;
use figma_import::{fetch_doc, toolkit_schema, ConvertRequest, ProxyConfig};
use jni::objects::{JByteArray, JClass, JObject, JString, JValue, JValueGen};
use jni::sys::{jboolean, jint, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM};
use layout::{
    add_view, add_view_measure, compute_layout, get_node_layout, remove_view, set_node_size,
    unchanged_response,
};
use lazy_static::lazy_static;
use log::{error, info, warn, LevelFilter};
use std::sync::{Mutex, MutexGuard};

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
        Err(_) => ProxyConfig::None,
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

fn jni_get_layout<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    layout_id: jint,
) -> JByteArray<'local> {
    let layout = get_node_layout(layout_id);
    if let Some(layout) = layout {
        let bytes: Result<Vec<u8>, figma_import::Error> =
            bincode::serialize(&layout).map_err(|e| e.into());
        match bytes {
            Ok(bytes) => env.byte_array_from_slice(&bytes).unwrap_or_else(|_| {
                throw_basic_exception(&mut env, "Internal JNI Error".to_string());
                JObject::null().into()
            }),
            Err(_err) => {
                throw_basic_exception(&mut env, "Layout serialization error".to_string());
                JObject::null().into()
            }
        }
    } else {
        warn!("Layout not found for layout_id {}", layout_id);
        JObject::null().into()
    }
}

fn layout_response_to_bytearray<'local>(
    mut env: JNIEnv<'local>,
    layout_response: &LayoutChangedResponse,
) -> JByteArray<'local> {
    let bytes: Result<Vec<u8>, figma_import::Error> =
        bincode::serialize(layout_response).map_err(|e| e.into());
    match bytes {
        Ok(bytes) => env.byte_array_from_slice(&bytes).unwrap_or_else(|_| {
            throw_basic_exception(&mut env, "Internal JNI Error".to_string());
            JObject::null().into()
        }),
        Err(_err) => {
            throw_basic_exception(&mut env, "Layout serialization error".to_string());
            JObject::null().into()
        }
    }
}

fn jni_set_node_size<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    layout_id: jint,
    width: jint,
    height: jint,
) -> JByteArray<'local> {
    let layout_response = set_node_size(layout_id, width as u32, height as u32);
    layout_response_to_bytearray(env, &layout_response)
}

fn jni_add_node<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    layout_id: jint,
    parent_layout_id: jint,
    child_index: jint,
    jserialized_view: JByteArray,
    jserialized_variant_view: JByteArray,
    compute_layout: jboolean,
) -> JByteArray<'local> {
    let bytes_view = env.convert_byte_array(jserialized_view);
    if let Ok(bytes_view) = bytes_view {
        let result: Result<toolkit_schema::View, Box<bincode::ErrorKind>> =
            bincode::deserialize(&bytes_view);
        match result {
            Ok(view) => {
                let mut variant_view: Option<toolkit_schema::View> = None;
                let bytes_variant_view: Result<Vec<u8>, jni::errors::Error> =
                    env.convert_byte_array(jserialized_variant_view);
                if let Ok(bytes_variant_view) = bytes_variant_view {
                    if !bytes_variant_view.is_empty() {
                        let var_result: Result<toolkit_schema::View, Box<bincode::ErrorKind>> =
                            bincode::deserialize(&bytes_variant_view);
                        match var_result {
                            Ok(var_view) => variant_view = Some(var_view),
                            Err(e) => throw_basic_exception(
                                &mut env,
                                format!("Internal JNI Error: {}", e),
                            ),
                        }
                    }
                }

                let layout_response = add_view(
                    layout_id,
                    parent_layout_id,
                    child_index,
                    view,
                    variant_view,
                    compute_layout != 0,
                );
                return layout_response_to_bytearray(env, &layout_response);
            }
            Err(e) => {
                throw_basic_exception(&mut env, format!("Internal JNI Error: {}", e));
            }
        }
    }
    layout_response_to_bytearray(env, &unchanged_response())
}

fn jni_add_text_node<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    layout_id: jint,
    parent_layout_id: jint,
    child_index: jint,
    jserialized_view: JByteArray,
    compute_layout: jboolean,
) -> JByteArray<'local> {
    let bytes_view = env.convert_byte_array(jserialized_view);
    if let Ok(bytes_view) = bytes_view {
        let result: Result<toolkit_schema::View, Box<bincode::ErrorKind>> =
            bincode::deserialize(&bytes_view);
        match result {
            Ok(view) => {
                let layout_response = add_view_measure(
                    layout_id,
                    parent_layout_id,
                    child_index,
                    view,
                    java_jni_measure_text,
                    compute_layout != 0,
                );
                return layout_response_to_bytearray(env, &layout_response);
            }
            Err(e) => {
                throw_basic_exception(&mut env, format!("Internal JNI Error: {}", e));
            }
        }
    }
    layout_response_to_bytearray(env, &unchanged_response())
}

fn jni_remove_node<'local>(
    env: JNIEnv<'local>,
    _class: JClass,
    layout_id: jint,
) -> JByteArray<'local> {
    let layout_response = remove_view(layout_id);
    layout_response_to_bytearray(env, &layout_response)
}

fn jni_compute_layout<'local>(env: JNIEnv<'local>, _class: JClass) -> JByteArray<'local> {
    info!("jni_compute_layout");

    let layout_response = compute_layout();
    layout_response_to_bytearray(env, &layout_response)
}

lazy_static! {
    static ref JAVA_VM: Mutex<Option<JavaVM>> = Mutex::new(None);
}

fn javavm() -> MutexGuard<'static, Option<JavaVM>> {
    JAVA_VM.lock().unwrap()
}

fn get_text_size(env: &mut JNIEnv, input: &JObject) -> Result<(f32, f32), jni::errors::Error> {
    let width = env.get_field(input, "width", "F")?.f()?;
    let height = env.get_field(input, "height", "F")?.f()?;
    Ok((width, height))
}

pub fn java_jni_measure_text(
    layout_id: i32,
    width: f32,
    height: f32,
    available_width: f32,
    available_height: f32,
) -> (f32, f32) {
    let vm = javavm();
    if let Some(vm) = vm.as_ref() {
        let mut env: JNIEnv<'_> = vm.get_env().expect("Cannot get reference to the JNIEnv");
        let class_result = env.find_class("com/android/designcompose/DesignTextMeasure");
        match class_result {
            Ok(jclass) => {
                let call_result = env.call_static_method(
                    jclass,
                    "measureTextSize",
                    "(IFFFF)Lcom/android/designcompose/TextSize;",
                    &[
                        JValue::from(layout_id),
                        JValue::from(width),
                        JValue::from(height),
                        JValue::from(available_width),
                        JValue::from(available_height),
                    ],
                );
                match &call_result {
                    Ok(text_size_object) => {
                        if let JValueGen::Object(o) = text_size_object {
                            let text_size_result = get_text_size(&mut env, o);
                            match text_size_result {
                                Ok(text_size) => return text_size,
                                Err(e) => {
                                    error!("JNI get_text_size failed: {}", e);
                                }
                            }
                        }
                    }
                    Err(e) => {
                        error!("Java JNI measureTextSize() error: {:?}", e);
                    }
                }
            }
            Err(e) => {
                error!("Java JNI failed to find class DesignTextMeasure: {}", e);
            }
        }
    } else {
        error!("Java JNI failed to get JavaVM: {:?}", vm);
    }
    (0.0, 0.0)
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: JavaVM, _: *mut c_void) -> jint {
    // Enable the logger, limit the log level to info to reduce spam
    android_logger::init_once(Config::default().with_tag("Jni").with_max_level(LevelFilter::Info));

    let mut env: JNIEnv<'_> = vm.get_env().expect("Cannot get reference to the JNIEnv");

    let cls = env
        .find_class("com/android/designcompose/Jni")
        .expect("Unable to find com.android.designcompose.Jni class");

    if env
        .register_native_methods(
            cls,
            &[
                jni::NativeMethod {
                    name: "jniFetchDoc".into(),
                    sig: "(Ljava/lang/String;Ljava/lang/String;Lcom/android/designcompose/ProxyConfig;)[B".into(),
                    fn_ptr: jni_fetch_doc as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniGetLayout".into(),
                    sig: "(I)[B".into(),
                    fn_ptr: jni_get_layout as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniSetNodeSize".into(),
                    sig: "(III)[B".into(),
                    fn_ptr: jni_set_node_size as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniAddNode".into(),
                    sig: "(III[B[BZ)[B".into(),
                    fn_ptr: jni_add_node as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniAddTextNode".into(),
                    sig: "(III[BZ)[B".into(),
                    fn_ptr: jni_add_text_node as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniRemoveNode".into(),
                    sig: "(I)[B".into(),
                    fn_ptr: jni_remove_node as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniComputeLayout".into(),
                    sig: "()[B".into(),
                    fn_ptr: jni_compute_layout as *mut c_void,
                }
            ],
        )
        .is_err()
    {
        error!("Unable to register native methods");
    }

    // Save the Java VM so we can call back into Android
    {
        let mut javavm = javavm();
        *javavm = Some(vm);
    }

    JNI_VERSION_1_6
}

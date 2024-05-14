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

use std::collections::HashMap;
use std::ffi::c_void;
use std::sync::atomic::{AtomicI32, Ordering};
use std::sync::{Arc, Mutex, MutexGuard};

use crate::error::{throw_basic_exception, Error};
use crate::error_map::map_err_to_exception;
use android_logger::Config;
use figma_import::{fetch_doc, ConvertRequest, ProxyConfig};
use jni::objects::{JByteArray, JClass, JObject, JString, JValue, JValueGen};
use jni::sys::{jboolean, jint, JNI_VERSION_1_6};
use jni::{JNIEnv, JavaVM};
use layout::layout_node::{LayoutNodeList, LayoutParentChildren};
use layout::{LayoutChangedResponse, LayoutManager};
use lazy_static::lazy_static;
use log::{error, info, LevelFilter};

lazy_static! {
    static ref JAVA_VM: Mutex<Option<JavaVM>> = Mutex::new(None);
    static ref LAYOUT_MANAGERS: Mutex<HashMap<i32, Arc<Mutex<LayoutManager>>>> =
        Mutex::new(HashMap::new());
}
static LAYOUT_MANAGER_ID: AtomicI32 = AtomicI32::new(0);

fn javavm() -> MutexGuard<'static, Option<JavaVM>> {
    JAVA_VM.lock().unwrap()
}

fn create_layout_manager() -> i32 {
    let manager = Arc::new(Mutex::new(LayoutManager::new()));
    let mut hash = LAYOUT_MANAGERS.lock().unwrap();

    let manager_id = LAYOUT_MANAGER_ID.fetch_add(1, Ordering::SeqCst);
    hash.insert(manager_id, manager);
    manager_id
}

fn manager(id: i32) -> Option<Arc<Mutex<LayoutManager>>> {
    let managers = LAYOUT_MANAGERS.lock().unwrap();
    let manager = managers.get(&id);
    if let Some(manager) = manager {
        Some(manager.clone())
    } else {
        None
    }
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
        Err(err) => {
            throw_basic_exception(&mut env, &err);
            return JObject::null().into();
        }
    };

    let request_json: String = match env.get_string(&jrequest_json) {
        Ok(it) => it.into(),
        Err(err) => {
            throw_basic_exception(&mut env, &err);
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

    match env.byte_array_from_slice(&ser_result) {
        Ok(it) => it,
        Err(err) => {
            throw_basic_exception(&mut env, &err);
            JObject::null().into()
        }
    }
}

fn jni_fetch_doc_impl(
    env: &mut JNIEnv,
    doc_id: String,
    request_json: String,
    proxy_config: &ProxyConfig,
) -> Result<Vec<u8>, Error> {
    let request: ConvertRequest = serde_json::from_str(&request_json)?;

    let convert_result: figma_import::ConvertResponse =
        match fetch_doc(&doc_id, request, proxy_config).map_err(Error::from) {
            Ok(it) => it,
            Err(err) => {
                map_err_to_exception(env, &err, doc_id).expect("Failed to throw exception");
                return Err(err);
            }
        };

    bincode::serialize(&convert_result).map_err(|e| e.into())
}

fn layout_response_to_bytearray<'local>(
    mut env: JNIEnv<'local>,
    layout_response: &LayoutChangedResponse,
) -> JByteArray<'local> {
    let bytes: Result<Vec<u8>, Error> = bincode::serialize(layout_response).map_err(|e| e.into());
    match bytes {
        Ok(bytes) => env.byte_array_from_slice(&bytes).unwrap_or_else(|_| {
            throw_basic_exception(&mut env, &Error::GenericError("Internal JNI Error".to_string()));
            JObject::null().into()
        }),
        Err(err) => {
            throw_basic_exception(&mut env, &err);
            JObject::null().into()
        }
    }
}

fn jni_create_layout_manager<'local>(_env: JNIEnv<'local>, _class: JClass) -> i32 {
    create_layout_manager()
}

fn jni_set_node_size<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    manager_id: jint,
    layout_id: jint,
    root_layout_id: jint,
    width: jint,
    height: jint,
) -> JByteArray<'local> {
    let manager = manager(manager_id);
    if let Some(manager_ref) = manager {
        let mut mgr = manager_ref.lock().unwrap();
        let layout_response =
            mgr.set_node_size(layout_id, root_layout_id, width as u32, height as u32);
        layout_response_to_bytearray(env, &layout_response)
    } else {
        throw_basic_exception(
            &mut env,
            &Error::GenericError(format!("No manager with id {}", manager_id)),
        );
        JObject::null().into()
    }
}

fn jni_add_nodes<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    manager_id: jint,
    root_layout_id: jint,
    serialized_views: JByteArray,
) -> JByteArray<'local> {
    let manager = manager(manager_id);
    if let Some(manager_ref) = manager {
        let mut manager = manager_ref.lock().unwrap();
        match env.convert_byte_array(serialized_views) {
            Ok(bytes_views) => {
                let result: Result<LayoutNodeList, Box<bincode::ErrorKind>> =
                    bincode::deserialize(&bytes_views);
                match result {
                    Ok(node_list) => {
                        info!(
                            "jni_add_nodes: {} nodes, layout_id {}",
                            node_list.layout_nodes.len(),
                            root_layout_id
                        );
                        for node in node_list.layout_nodes.into_iter() {
                            if node.use_measure_func {
                                manager.add_style_measure(
                                    node.layout_id,
                                    node.parent_layout_id,
                                    node.child_index,
                                    node.style,
                                    node.name,
                                    java_jni_measure_text,
                                );
                            } else {
                                manager.add_style(
                                    node.layout_id,
                                    node.parent_layout_id,
                                    node.child_index,
                                    node.style,
                                    node.name,
                                    None,
                                    node.fixed_width,
                                    node.fixed_height,
                                );
                            }
                        }
                        for LayoutParentChildren { parent_layout_id, child_layout_ids } in
                            &node_list.parent_children
                        {
                            manager.update_children(*parent_layout_id, child_layout_ids)
                        }
                    }
                    Err(e) => {
                        throw_basic_exception(&mut env, &e);
                    }
                }
            }
            Err(e) => {
                throw_basic_exception(&mut env, &e);
            }
        }

        let layout_response = manager.compute_node_layout(root_layout_id);
        layout_response_to_bytearray(env, &layout_response)
    } else {
        throw_basic_exception(
            &mut env,
            &Error::GenericError(format!("No manager with id {}", manager_id)),
        );
        JObject::null().into()
    }
}

fn jni_remove_node<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    manager_id: jint,
    layout_id: jint,
    root_layout_id: jint,
    compute_layout: jboolean,
) -> JByteArray<'local> {
    let manager = manager(manager_id);
    if let Some(manager_ref) = manager {
        let mut manager = manager_ref.lock().unwrap();
        let layout_response = manager.remove_view(layout_id, root_layout_id, compute_layout != 0);
        layout_response_to_bytearray(env, &layout_response)
    } else {
        throw_basic_exception(
            &mut env,
            &Error::GenericError(format!("No manager with id {}", manager_id)),
        );
        JObject::null().into()
    }
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
                    name: "jniCreateLayoutManager".into(),
                    sig: "()I".into(),
                    fn_ptr: jni_create_layout_manager as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniSetNodeSize".into(),
                    sig: "(IIIII)[B".into(),
                    fn_ptr: jni_set_node_size as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniAddNodes".into(),
                    sig: "(II[B)[B".into(),
                    fn_ptr: jni_add_nodes as *mut c_void,
                },
                jni::NativeMethod {
                    name: "jniRemoveNode".into(),
                    sig: "(IIIZ)[B".into(),
                    fn_ptr: jni_remove_node as *mut c_void,
                },
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

// Copyright 2024 Google LLC
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

use std::convert::TryFrom;

use std::sync::atomic::{AtomicI32, Ordering};
use std::sync::{Arc, Mutex, MutexGuard};

use bytes::Bytes;
use jni::objects::{JByteArray, JClass, JObject, JValue, JValueGen};
use jni::sys::{jboolean, jint};
use jni::JNIEnv;
use layout::layout_node::{LayoutNodeList, LayoutParentChildren};
use layout::{LayoutChangedResponse, LayoutManager};
use lazy_static::lazy_static;
use log::{error, info};
use prost::Message;

use crate::error::{throw_basic_exception, Error, Error::GenericError};
use crate::jni::javavm;

lazy_static! {
    static ref LAYOUT_MANAGERS: Mutex<HashMap<i32, Arc<Mutex<LayoutManager>>>> =
        Mutex::new(HashMap::new());
}

static LAYOUT_MANAGER_ID: AtomicI32 = AtomicI32::new(0);

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
    manager.map(|manager| manager.clone())
}

fn layout_response_to_bytearray(
    mut env: JNIEnv,
    layout_response: LayoutChangedResponse,
) -> JByteArray {
    let proto_msg: layout::proto::LayoutChangedResponse = layout_response.into();

    let bytes = proto_msg.encode_to_vec();
    match env.byte_array_from_slice(bytes.as_slice()) {
        Ok(it) => it,
        Err(err) => {
            throw_basic_exception(&mut env, &err);
            JObject::null().into()
        }
    }
}

pub(crate) fn jni_create_layout_manager(_env: JNIEnv, _class: JClass) -> i32 {
    create_layout_manager()
}

pub(crate) fn jni_set_node_size<'local>(
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
        layout_response_to_bytearray(env, layout_response)
    } else {
        throw_basic_exception(
            &mut env,
            &GenericError(format!("No manager with id {}", manager_id)),
        );
        JObject::null().into()
    }
}

pub(crate) fn jni_add_nodes<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
    manager_id: jint,
    root_layout_id: jint,
    serialized_views: JByteArray,
) -> JByteArray<'local> {
    let manager_ref = match manager(manager_id) {
        Some(it) => it,
        None => {
            throw_basic_exception(
                &mut env,
                &GenericError(format!("No manager with id {}", manager_id)),
            );
            return JObject::null().into();
        }
    };

    let mut manager = manager_ref.lock().unwrap();

    fn deprotolize_layout_node_list(
        env: &mut JNIEnv,
        serialized_views: JByteArray,
    ) -> Result<layout::layout_node::LayoutNodeList, Error> {
        let bytes_views: Bytes = env.convert_byte_array(serialized_views)?.into();
        let proto_msg = layout::proto::LayoutNodeList::decode(bytes_views).map_err(Error::from)?;
        LayoutNodeList::try_from(proto_msg).map_err(Error::from)
    }

    match deprotolize_layout_node_list(&mut env, serialized_views) {
        Ok(node_list) => {
            info!(
                "jni_add_nodes: {} nodes, layout_id {}",
                node_list.layout_nodes.len(),
                root_layout_id
            );
            handle_layout_node_list(node_list, root_layout_id, &mut manager);

            let layout_response = manager.compute_node_layout(root_layout_id);
            layout_response_to_bytearray(env, layout_response)
        }
        Err(e) => {
            throw_basic_exception(&mut env, &e);
            JObject::null().into()
        }
    }
}

fn handle_layout_node_list(
    node_list: LayoutNodeList,
    root_layout_id: i32,
    manager: &mut MutexGuard<LayoutManager>,
) {
    info!("jni_add_nodes: {} nodes, layout_id {}", node_list.layout_nodes.len(), root_layout_id);
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
    for LayoutParentChildren { parent_layout_id, child_layout_ids } in &node_list.parent_children {
        manager.update_children(*parent_layout_id, child_layout_ids)
    }
}

pub(crate) fn jni_remove_node<'local>(
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
        layout_response_to_bytearray(env, layout_response)
    } else {
        throw_basic_exception(
            &mut env,
            &GenericError(format!("No manager with id {}", manager_id)),
        );
        JObject::null().into()
    }
}

fn get_text_size(env: &mut JNIEnv, input: &JObject) -> Result<(f32, f32), jni::errors::Error> {
    let width = env.get_field(input, "width", "F")?.f()?;
    let height = env.get_field(input, "height", "F")?.f()?;
    Ok((width, height))
}

pub(crate) fn java_jni_measure_text(
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

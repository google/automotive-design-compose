// Copyright 2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the \"License\");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an \"AS IS\" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::collections::HashMap;
use std::sync::atomic::{AtomicI32, Ordering};
use std::sync::{Arc, Mutex, MutexGuard};

use dc_bundle::jni_layout::{LayoutChangedResponse, LayoutNodeList, LayoutParentChildren};
use dc_layout::LayoutManager;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JByteArray, JClass, JObject, JValue, JValueOwned};
use jni::sys::{jboolean, jint};
use jni::{Env, EnvUnowned};
use log::{error, info};
use std::sync::LazyLock;

use crate::error::{throw_basic_exception, Error, Error::GenericError};
use crate::jni::javavm;

use protobuf::Message;

static LAYOUT_MANAGERS: LazyLock<Mutex<HashMap<i32, Arc<Mutex<LayoutManager>>>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

static LAYOUT_MANAGER_ID: AtomicI32 = AtomicI32::new(0);

fn create_layout_manager() -> i32 {
    let manager = Arc::new(Mutex::new(LayoutManager::new(java_jni_measure_text)));
    let mut hash = LAYOUT_MANAGERS.lock().unwrap();

    let manager_id = LAYOUT_MANAGER_ID.fetch_add(1, Ordering::SeqCst);
    hash.insert(manager_id, manager);
    manager_id
}

fn manager(id: i32) -> Option<Arc<Mutex<LayoutManager>>> {
    let managers = LAYOUT_MANAGERS.lock().unwrap();
    let manager = managers.get(&id);
    manager.cloned()
}

fn layout_response_to_bytearray<'local>(
    env: &mut Env<'local>,
    layout_response: LayoutChangedResponse,
) -> JByteArray<'local> {
    let mut bytes: Vec<u8> = vec![];
    if let Err(err) = layout_response.write_length_delimited_to_vec(&mut bytes) {
        throw_basic_exception(env, &err);
        return JByteArray::default();
    }
    match env.byte_array_from_slice(bytes.as_slice()) {
        Ok(it) => it,
        Err(err) => {
            throw_basic_exception(env, &err);
            JByteArray::default()
        }
    }
}

pub(crate) fn jni_create_layout_manager(_env: EnvUnowned, _class: JClass) -> i32 {
    create_layout_manager()
}

pub(crate) fn jni_set_node_size<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass,
    manager_id: jint,
    layout_id: jint,
    root_layout_id: jint,
    width: jint,
    height: jint,
) -> JByteArray<'local> {
    env.with_env(|env| {
        let manager = manager(manager_id);
        if let Some(manager_ref) = manager {
            let mut mgr = manager_ref.lock().unwrap();
            let layout_response =
                mgr.set_node_size(layout_id, root_layout_id, width as u32, height as u32);
            Ok(layout_response_to_bytearray(env, layout_response))
        } else {
            throw_basic_exception(env, &GenericError(format!("No manager with id {}", manager_id)));
            Ok::<_, jni::errors::Error>(JByteArray::default())
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) fn jni_add_nodes<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass,
    manager_id: jint,
    root_layout_id: jint,
    serialized_views: JByteArray,
) -> JByteArray<'local> {
    env.with_env(|env| {
        let manager_ref = match manager(manager_id) {
            Some(it) => it,
            None => {
                throw_basic_exception(
                    env,
                    &GenericError(format!("No manager with id {}", manager_id)),
                );
                return Ok::<_, jni::errors::Error>(JByteArray::default());
            }
        };

        let mut manager = manager_ref.lock().unwrap();

        fn deprotolize_layout_node_list(
            env: &mut Env,
            serialized_views: JByteArray,
        ) -> Result<LayoutNodeList, Error> {
            let bytes_views: Vec<u8> = env.convert_byte_array(serialized_views)?;
            LayoutNodeList::parse_from_bytes(bytes_views.as_slice()).map_err(Error::from)
        }

        match deprotolize_layout_node_list(env, serialized_views) {
            Ok(node_list) => {
                if let Err(e) = handle_layout_node_list(node_list, &mut manager) {
                    info!("jni_add_nodes: Error handling layout node list: {:?}", e);
                    throw_basic_exception(env, &e);
                    return Ok::<_, jni::errors::Error>(JByteArray::default());
                }

                let layout_response = manager.compute_node_layout(root_layout_id);
                Ok(layout_response_to_bytearray(env, layout_response))
            }
            Err(e) => {
                info!("jni_add_nodes: failed with error {:?}", e);
                throw_basic_exception(env, &e);
                Ok::<_, jni::errors::Error>(JByteArray::default())
            }
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

fn handle_layout_node_list(
    node_list: LayoutNodeList,
    manager: &mut MutexGuard<LayoutManager>,
) -> Result<(), Error> {
    for node in node_list.layout_nodes.into_iter() {
        manager.add_style(
            node.layout_id,
            node.parent_layout_id,
            node.child_index,
            node.style.into_option().expect("Malformed Data, style is required"),
            node.name,
            node.use_measure_func,
            if node.use_measure_func { None } else { node.fixed_width },
            if node.use_measure_func { None } else { node.fixed_height },
        )?;
    }
    for LayoutParentChildren { parent_layout_id, child_layout_ids, .. } in
        &node_list.parent_children
    {
        manager.update_children(*parent_layout_id, child_layout_ids)
    }
    Ok(())
}

pub(crate) fn jni_remove_node<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass,
    manager_id: jint,
    layout_id: jint,
    root_layout_id: jint,
    compute_layout: jboolean,
) -> JByteArray<'local> {
    env.with_env(|env| {
        let manager = manager(manager_id);
        if let Some(manager_ref) = manager {
            let mut manager = manager_ref.lock().unwrap();
            let layout_response = manager.remove_view(layout_id, root_layout_id, compute_layout);
            Ok(layout_response_to_bytearray(env, layout_response))
        } else {
            throw_basic_exception(env, &GenericError(format!("No manager with id {}", manager_id)));
            Ok::<_, jni::errors::Error>(JByteArray::default())
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) fn jni_mark_dirty<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass,
    manager_id: jint,
    layout_id: jint,
) {
    env.with_env(|env| -> Result<(), jni::errors::Error> {
        if let Some(manager_ref) = manager(manager_id) {
            let mut manager = manager_ref.lock().unwrap();
            manager.mark_dirty(layout_id);
        } else {
            throw_basic_exception(env, &GenericError(format!("No manager with id {}", manager_id)));
        }
        Ok(())
    })
    .resolve::<ThrowRuntimeExAndDefault>();
}

fn get_text_size(env: &mut Env, input: &JObject) -> Result<(f32, f32), jni::errors::Error> {
    let f_sig: jni::signature::RuntimeFieldSignature = "F".parse().unwrap();
    let width = env
        .get_field(input, jni::strings::JNIString::from("width"), f_sig.field_signature())?
        .f()?;
    let height = env
        .get_field(input, jni::strings::JNIString::from("height"), f_sig.field_signature())?
        .f()?;
    Ok((width, height))
}

pub(crate) fn java_jni_measure_text(
    layout_id: i32,
    width: f32,
    height: f32,
    available_width: f32,
    available_height: f32,
) -> (f32, f32) {
    if let Some(vm) = javavm() {
        let result = vm.attach_current_thread(|env| -> Result<(f32, f32), jni::errors::Error> {
            let jclass = env.find_class(jni::strings::JNIString::from(
                "com/android/designcompose/DesignTextMeasure",
            ))?;
            let sig: jni::signature::RuntimeMethodSignature =
                "(IFFFF)Lcom/android/designcompose/TextSize;".parse().unwrap();
            let call_result = env.call_static_method(
                jclass,
                jni::strings::JNIString::from("measureTextSize"),
                sig.method_signature(),
                &[
                    JValue::from(layout_id),
                    JValue::from(width),
                    JValue::from(height),
                    JValue::from(available_width),
                    JValue::from(available_height),
                ],
            )?;
            if let JValueOwned::Object(o) = call_result {
                get_text_size(env, &o)
            } else {
                Err(jni::errors::Error::JavaException)
            }
        });
        match result {
            Ok(size) => return size,
            Err(e) => error!("Java JNI measureTextSize() error: {:?}", e),
        }
    } else {
        error!("Java JNI failed to get JavaVM");
    }
    (0.0, 0.0)
}

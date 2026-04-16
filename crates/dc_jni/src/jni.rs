// Copyright 2023 Google LLC
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

use std::ffi::c_void;

use std::sync::{Arc, Mutex};

use crate::android_interface::convert_request::fetch_doc;
use crate::error::{throw_basic_exception, Error};
use crate::error_map::map_err_to_exception;
use crate::layout_manager::{
    jni_add_nodes, jni_create_layout_manager, jni_mark_dirty, jni_remove_node, jni_set_node_size,
};
use android_logger::Config;
use dc_figma_import::ProxyConfig;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JByteArray, JClass, JObject, JString};
use jni::sys::{jint, JNI_VERSION_1_6};
use jni::{Env, EnvUnowned, JavaVM};

use dc_bundle::android_interface::{ConvertRequest, ConvertResponse};
use log::{error, info, LevelFilter};
use protobuf::Message;
use std::sync::LazyLock;

static JAVA_VM: LazyLock<Mutex<Option<Arc<JavaVM>>>> = LazyLock::new(|| Mutex::new(None));

pub fn javavm() -> Option<Arc<JavaVM>> {
    JAVA_VM.lock().unwrap().clone()
}
fn set_javavm(vm: JavaVM) {
    *JAVA_VM.lock().unwrap() = Some(Arc::new(vm))
}

fn get_string(env: &mut Env, obj: &JObject) -> Option<String> {
    if obj.is_null() {
        return None;
    }
    let jstr = unsafe { JString::from_raw(env, obj.as_raw()) };
    jstr.mutf8_chars(env).ok().map(|s| s.into())
}

fn get_proxy_config(env: &mut Env, input: &JObject) -> Result<ProxyConfig, jni::errors::Error> {
    let f_sig: jni::signature::RuntimeFieldSignature =
        "Lcom/android/designcompose/HttpProxyConfig;".parse().unwrap();
    let http_proxy_config = env
        .get_field(
            input,
            jni::strings::JNIString::from("httpProxyConfig"),
            f_sig.field_signature(),
        )?
        .l()?;

    let f_sig_str: jni::signature::RuntimeFieldSignature = "Ljava/lang/String;".parse().unwrap();
    let proxy_spec_field = env
        .get_field(
            &http_proxy_config,
            jni::strings::JNIString::from("proxySpec"),
            f_sig_str.field_signature(),
        )?
        .l()?;
    Ok(match get_string(env, &proxy_spec_field) {
        Some(x) => ProxyConfig::HttpProxyConfig(x),
        None => ProxyConfig::None,
    })
}

fn jni_fetch_doc<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass,
    jdoc_id: JString,
    jversion_id: JString,
    jrequest: JByteArray,
    jproxy_config: JObject,
) -> JByteArray<'local> {
    env.with_env(|env| {
        let doc_id: String = match env.get_string(&jdoc_id) {
            Ok(it) => it.into(),
            Err(err) => {
                throw_basic_exception(env, &err);
                return Ok::<_, jni::errors::Error>(JByteArray::default());
            }
        };

        let version_id: String = match env.get_string(&jversion_id) {
            Ok(it) => it.into(),
            Err(err) => {
                throw_basic_exception(env, &err);
                return Ok::<_, jni::errors::Error>(JByteArray::default());
            }
        };

        let request_bytes: Vec<u8> = match env.convert_byte_array(&jrequest) {
            Ok(it) => it,
            Err(err) => {
                throw_basic_exception(env, &err);
                return Ok::<_, jni::errors::Error>(JByteArray::default());
            }
        };

        let mut request: ConvertRequest = ConvertRequest::new();
        if let Err(err) = request.merge_from_bytes(&request_bytes).map_err(Error::from) {
            throw_basic_exception(env, &err);
        };

        let proxy_config: ProxyConfig = match get_proxy_config(env, &jproxy_config) {
            Ok(it) => it,
            Err(_) => ProxyConfig::None,
        };

        let ser_result = match jni_fetch_doc_impl(env, doc_id, version_id, request, &proxy_config) {
            Ok(it) => it,
            Err(_err) => {
                return Ok::<_, jni::errors::Error>(JByteArray::default());
            }
        };

        match env.byte_array_from_slice(&ser_result) {
            Ok(it) => Ok(it),
            Err(err) => {
                throw_basic_exception(env, &err);
                Ok::<_, jni::errors::Error>(JByteArray::default())
            }
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

fn jni_fetch_doc_impl(
    env: &mut Env,
    doc_id: String,
    version_id: String,
    request: ConvertRequest,
    proxy_config: &ProxyConfig,
) -> Result<Vec<u8>, Error> {
    let convert_result: ConvertResponse =
        match fetch_doc(&doc_id, &version_id, &request, proxy_config) {
            Ok(it) => it,
            Err(err) => {
                let queries_string = request
                    .queries
                    .iter()
                    .map(|q| format!("--nodes=\"{}\" ", q))
                    .collect::<Vec<String>>()
                    .join(" ");

                info!("Failed to fetch {}, Try fetching locally", doc_id);
                info!("fetch --doc-id={} --version-id={} {} ", doc_id, version_id, queries_string);

                map_err_to_exception(env, &err, doc_id).expect("Failed to throw exception");
                return Err(err);
            }
        };

    convert_result
        .write_length_delimited_to_bytes()
        .map_err(|e| Error::ProtobufWriteError(format!("Failed to write convert_result: {}", e)))
}

#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(vm: *mut jni::sys::JavaVM, _: *mut c_void) -> jint {
    let vm = unsafe { JavaVM::from_raw(vm) };
    // Enable the logger, limit the log level to info to reduce spam
    android_logger::init_once(Config::default().with_tag("Jni").with_max_level(LevelFilter::Info));

    let res = vm.attach_current_thread(|env| -> Result<(), jni::errors::Error> {
        let cls = env.find_class(jni::strings::JNIString::from("com/android/designcompose/Jni"))?;
        unsafe {
            env.register_native_methods(
                cls,
                &[
                    jni::NativeMethod::from_raw_parts(
                        jni::strings::JNIStr::from_cstr_unchecked(c"jniFetchDoc"),
                        jni::strings::JNIStr::from_cstr_unchecked(c"(Ljava/lang/String;Ljava/lang/String;[BLcom/android/designcompose/ProxyConfig;)[B"),
                        jni_fetch_doc as *mut c_void,
                    ),
                    jni::NativeMethod::from_raw_parts(
                        jni::strings::JNIStr::from_cstr_unchecked(c"jniCreateLayoutManager"),
                        jni::strings::JNIStr::from_cstr_unchecked(c"()I"),
                        jni_create_layout_manager as *mut c_void,
                    ),
                    jni::NativeMethod::from_raw_parts(
                        jni::strings::JNIStr::from_cstr_unchecked(c"jniSetNodeSize"),
                        jni::strings::JNIStr::from_cstr_unchecked(c"(IIIII)[B"),
                        jni_set_node_size as *mut c_void,
                    ),
                    jni::NativeMethod::from_raw_parts(
                        jni::strings::JNIStr::from_cstr_unchecked(c"jniAddNodes"),
                        jni::strings::JNIStr::from_cstr_unchecked(c"(II[B)[B"),
                        jni_add_nodes as *mut c_void,
                    ),
                    jni::NativeMethod::from_raw_parts(
                        jni::strings::JNIStr::from_cstr_unchecked(c"jniRemoveNode"),
                        jni::strings::JNIStr::from_cstr_unchecked(c"(IIIZ)[B"),
                        jni_remove_node as *mut c_void,
                    ),
                    jni::NativeMethod::from_raw_parts(
                        jni::strings::JNIStr::from_cstr_unchecked(c"jniMarkDirty"),
                        jni::strings::JNIStr::from_cstr_unchecked(c"(II)V"),
                        jni_mark_dirty as *mut c_void,
                    )
                ],
            )
        }
    });

    if res.is_err() {
        error!("Unable to register native methods");
    }

    // Save the Java VM so we can call back into Android
    set_javavm(vm);

    JNI_VERSION_1_6
}

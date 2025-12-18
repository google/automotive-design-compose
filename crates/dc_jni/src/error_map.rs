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

use figma_import::Error::NetworkError;
use jni::JNIEnv;
use log::error;
use std::error::Error;

pub fn map_err_to_exception(
    env: &mut JNIEnv,
    err: &crate::error::Error,
    doc_id: String,
) -> Result<(), jni::errors::Error> {
    match err {
        crate::error::Error::FigmaImportError(NetworkError(network_error)) => {
            error!("Network Error: {}, {}", err, err.source().unwrap().to_string());

            if let Some(status) = network_error.status() {
                match status.as_u16() {
                    400 => env.throw_new(
                        "com/android/designcompose/FetchException",
                        format!("Bad request: {}", status),
                    )?,
                    403 => env.throw_new(
                        "com/android/designcompose/AccessDeniedException",
                        "Invalid Authentication Token",
                    )?,
                    404 => env.throw_new(
                        "com/android/designcompose/FigmaFileNotFoundException",
                        doc_id,
                    )?,
                    429 => env.throw_new(
                        "com/android/designcompose/RateLimitedException",
                        "Figma Rate Limit Exceeded",
                    )?,
                    500 => env.throw_new(
                        "com/android/designcompose/InternalFigmaErrorException",
                        "Figma.com internal error",
                    )?,
                    code => env.throw_new(
                        "com/android/designcompose/FetchException",
                        format!("Unhandled response from server: {}", code),
                    )?,
                }
            } else if network_error.is_connect() || network_error.is_timeout() {
                env.throw_new(
                    "java/net/ConnectException",
                    format!("Network error: {}", network_error),
                )?
            } else {
                env.throw_new(
                    "java/net/SocketException",
                    format!("Network error: {}", network_error),
                )?
            }
        }
        _ => {
            error!("Unhandled: {}, {}", err, err.source().unwrap().to_string());
            env.throw(format!("{}", err))?;
        }
    };
    Ok(())
}

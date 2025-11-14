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

use figma_import::Error::FigmaApiError;

pub fn map_err_to_exception(
    env: &mut JNIEnv,
    err: &crate::error::Error,
    doc_id: String,
) -> Result<(), jni::errors::Error> {
    match err {
        crate::error::Error::FigmaImportError(FigmaApiError(status, message)) => {
            error!("Figma API Error: {} {}", status, message);
            match status.as_u16() {
                400 => {
                    return env.throw_new(
                        "com/android/designcompose/FetchException",
                        format!("Bad request: {}", message),
                    )
                }
                403 => {
                    return env.throw_new(
                        "com/android/designcompose/AccessDeniedException",
                        "Invalid Authentication Token",
                    )
                }
                404 => {
                    return env
                        .throw_new("com/android/designcompose/FigmaFileNotFoundException", doc_id)
                }
                429 => {
                    return env.throw_new(
                        "com/android/designcompose/RateLimitedException",
                        "Figma Rate Limit Exceeded",
                    )
                }
                500 => {
                    return env.throw_new(
                        "com/android/designcompose/InternalFigmaErrorException",
                        "Figma.com internal error",
                    )
                }
                code => {
                    return env.throw_new(
                        "com/android/designcompose/FetchException",
                        format!("Unhandled response from server: {}: {}", code, message),
                    )
                }
            }
        }
        crate::error::Error::FigmaImportError(NetworkError(network_error)) => {
            error!("Network Error: {}, {}", err, err.source().unwrap().to_string());

            if network_error.is_timeout() {
                return env.throw_new(
                    "java/net/SocketTimeoutException",
                    "Timeout connecting to Figma.com",
                );
            }
            if network_error.is_connect() {
                return env.throw_new(
                    "java/net/ConnectException",
                    format!("Failed to connect to Figma.com: {}", network_error),
                );
            }
            env.throw_new("java/net/SocketException", format!("Network error: {}", network_error))?
        }
        _ => {
            error!("Unhandled: {}, {}", err, err.source().unwrap().to_string());
            env.throw(format!("{}", err))?;
        }
    };
    Ok(())
}

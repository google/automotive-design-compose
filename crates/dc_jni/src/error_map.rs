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
use ureq::Error::{Status, Transport};
use ureq::ErrorKind;

pub fn map_err_to_exception(
    env: &mut JNIEnv,
    err: &crate::error::Error,
    doc_id: String,
) -> Result<(), jni::errors::Error> {
    match err {
        crate::error::Error::FigmaImportError(NetworkError(network_error)) => {
            error!("Network Error: {}, {}", err, err.source().unwrap().to_string());

            match network_error {
                Status(400, response) => env.throw_new(
                    "com/android/designcompose/FetchException",
                    format!("Bad request: {}", response.status_text()),
                )?,
                Status(403, _) => env.throw_new(
                    "com/android/designcompose/AccessDeniedException",
                    "Invalid Authentication Token",
                )?,
                Status(404, _) => {
                    env.throw_new("com/android/designcompose/FigmaFileNotFoundException", doc_id)?
                }
                Status(429, _) => env.throw_new(
                    "com/android/designcompose/RateLimitedException",
                    "Figma Rate Limit Exceeded",
                )?,
                Status(500, _) => env.throw_new(
                    "com/android/designcompose/InternalFigmaErrorException",
                    "Figma.com internal error",
                )?,
                Status(code, response) => env.throw_new(
                    "com/android/designcompose/FetchException",
                    format!("Unhandled response from server: {}: {}", code, response.status_text()),
                )?,
                Transport(transport_error) => match transport_error.kind() {
                    ErrorKind::ConnectionFailed => env.throw_new(
                        "java/net/ConnectException",
                        transport_error.message().unwrap_or("No further details"),
                    )?,
                    kind => env.throw_new(
                        "java/net/SocketException",
                        format!("Network error: {}", kind),
                    )?,
                },
            }
        }
        _ => {
            error!("Unhandled: {}, {}", err, err.source().unwrap().to_string());
            env.throw(format!("{}", err))?;
        }
    };
    Ok(())
}

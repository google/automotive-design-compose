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

use jni::Env;
use thiserror::Error;
#[derive(Error, Debug)]
#[allow(clippy::enum_variant_names)]
pub enum Error {
    #[error("Error: {0}")]
    GenericError(String),
    #[error("Protobuf Error: {0:#?}")]
    ProtobufDecodeError(#[from] protobuf::Error),
    #[error("Protobuf ConversionError: {0}")]
    MissingFieldError(#[from] dc_bundle::Error),
    #[error("Json Serialization Error: {0}")]
    JsonError(#[from] serde_json::Error),
    #[error("DC_figma_import Error: {0}")]
    FigmaImportError(#[from] dc_figma_import::Error),
    #[error("JNI Error: {0}")]
    JNIError(#[from] jni::errors::Error),
    #[error("Protobuf Write Error: {0}")]
    ProtobufWriteError(String),
}

pub fn ack_throw(res: Result<(), jni::errors::Error>) -> Result<(), jni::errors::Error> {
    match res {
        Ok(()) | Err(jni::errors::Error::JavaException) => Ok(()),
        Err(err) => Err(err),
    }
}

#[allow(unused_qualifications)] // Linter thinks `std:` isn't needed but removing it fails the build
pub fn throw_basic_exception(env: &mut Env, err: &dyn std::error::Error) {
    // In jni 0.22+, `throw` returns `Err(Error::JavaException)` on a successful throw
    // so that the calling Rust code can unwind with `?`. It is NOT a failure to throw.
    // If throwing genuinely failed, log it instead of panicking to prevent aborting the host process.
    if let Err(e) = ack_throw(env.throw(err.to_string())) {
        log::error!("Failed to throw basic exception across JNI: {:?}", e);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ack_throw_ok() {
        assert!(ack_throw(Ok(())).is_ok());
    }

    #[test]
    fn test_ack_throw_java_exception_is_ok() {
        assert!(ack_throw(Err(jni::errors::Error::JavaException)).is_ok());
    }

    #[test]
    fn test_ack_throw_genuine_error_propagates() {
        let res = ack_throw(Err(jni::errors::Error::ThrowFailed(-1)));
        assert!(matches!(res, Err(jni::errors::Error::ThrowFailed(-1))));
    }
}

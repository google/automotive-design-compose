use figma_import::Error::NetworkError;
use jni::JNIEnv;
use log::error;
use std::error::Error;
use ureq::Error::{Status, Transport};
use ureq::ErrorKind;

pub fn map_err_to_exception(
    env: &mut JNIEnv,
    err: &figma_import::Error,
    doc_id: String,
) -> Result<(), jni::errors::Error> {
    match err {
        NetworkError(network_error) => {
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

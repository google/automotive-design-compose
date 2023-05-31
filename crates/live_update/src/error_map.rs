use figma_import::Error::NetworkError;
use jni::objects::JThrowable;
use jni::JNIEnv;
use log::{debug, error};
use ureq::Error::{Status, Transport};
use ureq::ErrorKind;

fn create_new_fetch_exception<'local>(
    env: &mut JNIEnv<'local>,
    class_name: &str,
    doc_id: String,
) -> Result<JThrowable<'local>, jni::errors::Error> {
    let doc_id_jstring = env.new_string(doc_id.to_string())?;
    let throwable: JThrowable = env
        .new_object(
            format!("com/android/designcompose/{class_name}"),
            "(Ljava/lang/String;)V",
            &[(&doc_id_jstring).into()],
        )?
        .into();
    return Ok(throwable);
}

pub fn map_err_to_exception(
    env: &mut JNIEnv,
    err: &figma_import::Error,
    doc_id: String,
) -> Result<(), jni::errors::Error> {
    match err {
        NetworkError(network_error) => match network_error {
            Status(403, _) => env.throw_new(
                "com/android/designcompose/AccessDeniedException",
                "Invalid Authentication Token",
            )?,
            Status(404, _) => {
                let exception =
                    create_new_fetch_exception(env, "DocumentNotFoundException", doc_id)?;
                env.throw(exception)?
            }
            Status(code, response) => env.throw_new(
                "com/android/designcompose/NetworkException",
                format!("Unhandled response from server: {}: {}", code, response.status_text()),
            )?,
            Transport(transport_error) => match transport_error.kind() {
                ErrorKind::ConnectionFailed => env.throw_new(
                    "com/android/designcompose/ConnectionFailedException",
                    transport_error.message().unwrap_or("No further details"),
                )?,

                _ => env.throw(format!(
                    "Unhandled network transport exception: {}",
                    transport_error.message().unwrap_or("No further details")
                ))?,
            },
        },

        _ => env.throw("Unhandled exception")?,
    };
    Ok(())
}

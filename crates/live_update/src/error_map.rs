use figma_import::Error::NetworkError;
use jni::objects::JThrowable;
use jni::JNIEnv;
use log::{debug, error};
use ureq::Error::{Status, Transport};
use ureq::ErrorKind;

pub fn map_err_to_exception(
    env: &mut JNIEnv,
    err: &figma_import::Error,
) -> Result<(), jni::errors::Error> {
    match err {
        NetworkError(network_error) => match network_error {
            Status(403, _) => env.throw_new(
                "com/android/designcompose/AccessDeniedException",
                "Invalid Authentication Token",
            )?,
            Status(404, resp) => {
                let doc_id = resp
                    .get_url()
                    .split("/")
                    .skip_while(|x| !x.eq(&"file"))
                    .next()
                    .unwrap_or("Unknown");
                error!("Got doc_id {doc_id}");
                let doc_id_jstring = env.new_string(doc_id.to_string())?;
                let exception: JThrowable = env
                    .new_object(
                        "com/android/designcompose/DocumentNotFoundException",
                        "(Ljava/lang/String;)V",
                        &[(&doc_id_jstring).into()],
                    )?
                    .into();
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

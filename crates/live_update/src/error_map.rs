use jni::JNIEnv;

use figma_import::Error;
use figma_import::Error::HttpError;
use ureq::Error::Status;

pub fn map_err_to_exception(env: &mut JNIEnv, err: &Error) {
    match err {
        HttpError(Status(403, _)) => env
            .throw_new(
                "com/android/designcompose/AccessDeniedException",
                "Invalid Authentication Token",
            )
            .expect("TODO: panic message"),
        _ => {
            env.throw("Unhandled exception exception").expect("TODO: panic message");
        }
    }
}

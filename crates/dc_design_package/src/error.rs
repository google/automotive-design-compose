use thiserror::Error;

/// Combined error type for all errors that can occur working with Figma documents.
#[derive(Error, Debug)]
pub enum Error {
    #[error("Serialization Error")]
    BincodeError(#[from] bincode::Error),
    #[error("Figma Document Load Error")]
    DocumentLoadError(String),
}
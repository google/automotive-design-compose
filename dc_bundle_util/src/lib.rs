use dc_bundle::definition_file::load_design_def;
use std::io;
use std::path::Path;

#[derive(Debug)]
pub enum Error {
    Io(io::Error),
    DcBundle(dc_bundle::Error),
    Protobuf(protobuf::Error), // Kept for potential future use
}

impl From<io::Error> for Error {
    fn from(err: io::Error) -> Error {
        Error::Io(err)
    }
}

impl From<dc_bundle::Error> for Error {
    fn from(err: dc_bundle::Error) -> Error {
        Error::DcBundle(err)
    }
}

impl From<protobuf::Error> for Error {
    fn from(err: protobuf::Error) -> Error {
        Error::Protobuf(err)
    }
}

/// Loads a DesignComposeDefinition from the given input path and returns its debug
/// string representation.
pub fn convert_dcd_to_textproto_str<P: AsRef<Path>>(input_path: P) -> Result<String, Error> {
    let (_header, design_def) = load_design_def(input_path)?;

    // Use the derived Debug trait for a human-readable output.
    let debug_string = format!("{:#?}", design_def);

    Ok(debug_string)
}

#[cfg(test)]
mod tests {
    use super::*;
    use dc_bundle::definition_file::save_design_def;
    use dc_bundle::design_compose_definition::{
        DesignComposeDefinition, DesignComposeDefinitionHeader,
    };
    use std::path::PathBuf;
    use tempfile::NamedTempFile;

    // Helper to create a dummy DCD file for testing
    fn create_dummy_dcd_file() -> io::Result<NamedTempFile> {
        let temp_file = NamedTempFile::new()?;
        let mut header = DesignComposeDefinitionHeader::new();
        header.dc_version = 1; // dc_version is u32
        let doc = DesignComposeDefinition::new();
        save_design_def(temp_file.path(), &header, &doc).expect("Failed to save dummy file");
        Ok(temp_file)
    }

    #[test]
    fn test_convert_dcd_to_textproto_str_success() {
        let input_file = create_dummy_dcd_file().unwrap();

        let result = convert_dcd_to_textproto_str(input_file.path());
        assert!(result.is_ok(), "convert_dcd_to_textproto_str failed: {:?}", result.err());

        let output_content = result.unwrap();
        assert!(!output_content.is_empty());
        // Check for the message name and braces, typical of Debug output for structs.
        assert!(output_content.contains("DesignComposeDefinition"));
        assert!(output_content.contains("{"));
        assert!(output_content.contains("}"));
    }

    #[test]
    fn test_convert_dcd_to_textproto_str_input_not_found() {
        let non_existent_path = PathBuf::from("non_existent_file");
        let result = convert_dcd_to_textproto_str(&non_existent_path);
        assert!(result.is_err());
        match result.err().unwrap() {
            Error::DcBundle(dc_bundle::Error::IoError(_)) => (), // Expected
            e => panic!("Unexpected error type: {:?}", e),
        }
    }
}

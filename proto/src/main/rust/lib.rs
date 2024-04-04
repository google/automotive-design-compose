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

pub mod dc_proto {
    include!(concat!(env!("OUT_DIR"), "/com.android.designcompose.proto.rs"));
}

#[cfg(test)]
mod tests {
    use std::io::{Read, Seek, SeekFrom, Write};

    use prost::Message;
    use tempfile::tempfile;

    use crate::dc_proto::TestMessage;

    #[test]
    fn write_and_read() {
        let mut test_message = TestMessage::default();
        test_message.number = 10;

        // Encode the message to a bytes buffer.
        let mut buf = Vec::new();
        test_message.encode(&mut buf).unwrap();

        // Write the bytes buffer to a file.
        let mut file = tempfile().unwrap();
        file.write_all(&buf).unwrap();
        println!("buf: {:?}", buf);

        file.seek(SeekFrom::Start(0)).unwrap();

        // Read the bytes buffer from the file.
        let mut file_contents = Vec::new();
        file.read_to_end(&mut file_contents).unwrap();
        let read_buf = file_contents.as_slice();

        // Decode the bytes buffer into a new message.
        let read_message = TestMessage::decode(read_buf).unwrap();

        // Assert that the new message is equal to the original message.
        assert_eq!(test_message, read_message);
    }
}

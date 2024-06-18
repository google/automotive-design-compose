/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use figma_import::image_context::{EncodedImageMap, ImageKey};
use std::collections::HashMap;
use std::sync::Arc;
use serde::{Deserialize, Serialize};

/// EncodedImageMap contains a mapping from ImageKey to network bytes. It can create an
/// ImageMap and is intended to be used when we want to use Figma-defined components but do
/// not want to communicate with the Figma service.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct EncodedImageMap(HashMap<ImageKey, Arc<serde_bytes::ByteBuf>>);

impl EncodedImageMap {
    pub fn map(&self) -> HashMap<ImageKey, Arc<serde_bytes::ByteBuf>> {
        self.0.clone()
    }
}

/// Instead of keeping decoded images in ViewStyle objects, we keep keys to the images in the
/// ImageContext and then fetch decoded images when rendering. This means we can serialize the
/// whole ImageContext, and always get the right image when we render.
#[derive(Clone, PartialEq, Eq, Hash, Serialize, Deserialize, Debug)]
pub struct ImageKey(String);

impl ImageKey {
    pub fn key(&self) -> String {
        self.0.clone()
    }

    pub fn new(str: String) -> ImageKey {
        ImageKey(str)
    }
}


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

// This module holds the original structures that made up the serialized design doc, and which will be replaced with the protobuf implementations of the Design Definition

// To help keep the legacy definition files clear we alias `crate::definition`, which is the base
// module for the generated protobuf files to `proto`, so that all of the protobuf-generated types
// inside `legacy_definition` must be prepended with `proto::`
pub(crate) use crate::definition as proto;
pub mod element;
pub mod layout;
pub mod modifier;

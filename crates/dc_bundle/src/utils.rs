// Copyright 2023 Google LLC
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

/// Compare two finite f32 values
/// This is not meant for situations dealing with NAN and INFINITY
#[inline]
pub(crate) fn f32_eq(a: &f32, b: &f32) -> bool {
    assert!(a.is_finite());
    assert!(b.is_finite());
    (a - b).abs() < f32::EPSILON
}

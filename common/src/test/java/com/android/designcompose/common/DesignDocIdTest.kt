/*
 * Copyright 2026 Google LLC
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

package com.android.designcompose.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesignDocIdTest {
    @Test
    fun testIsValid() {
        assertThat(DesignDocId("").isValid()).isFalse()
        assertThat(DesignDocId("test").isValid()).isTrue()
        assertThat(DesignDocId("test", "version").isValid()).isTrue()
    }

    @Test
    fun testToString() {
        assertThat(DesignDocId("test").toString()).isEqualTo("test")
        assertThat(DesignDocId("test", "version").toString()).isEqualTo("test_version")
        assertThat(DesignDocId("", "version").toString()).isEqualTo("_version")
        assertThat(DesignDocId("", "").toString()).isEqualTo("")
    }

    @Test
    fun testEquals() {
        assertThat(DesignDocId("test")).isEqualTo(DesignDocId("test"))
        assertThat(DesignDocId("test", "version")).isEqualTo(DesignDocId("test", "version"))
        assertThat(DesignDocId("test")).isNotEqualTo(DesignDocId("test2"))
        assertThat(DesignDocId("test", "version")).isNotEqualTo(DesignDocId("test", "version2"))
        assertThat(DesignDocId("test").equals(null)).isFalse()
        assertThat(DesignDocId("test").equals("test")).isFalse()
    }

    @Test
    fun testHashCode() {
        assertThat(DesignDocId("test").hashCode()).isEqualTo(DesignDocId("test").hashCode())
        assertThat(DesignDocId("test", "version").hashCode())
            .isEqualTo(DesignDocId("test", "version").hashCode())
        assertThat(DesignDocId("test").hashCode()).isNotEqualTo(DesignDocId("test2").hashCode())
        assertThat(DesignDocId("test", "version").hashCode())
    }

    @Test
    fun testFromUrl() {
        val idFromOldUrl = DesignDocId("https://www.figma.com/file/ABC123XYZ/MyFile")
        assertThat(idFromOldUrl.id).isEqualTo("ABC123XYZ")

        val idFromNewUrl = DesignDocId("https://www.figma.com/design/ABC123XYZ/MyFile")
        assertThat(idFromNewUrl.id).isEqualTo("ABC123XYZ")

        val idFromDirectUrl = DesignDocId("https://figma.com/file/ABC123XYZ")
        assertThat(idFromDirectUrl.id).isEqualTo("ABC123XYZ")
    }
}

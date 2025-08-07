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
            .isNotEqualTo(DesignDocId("test", "version2").hashCode())
    }
}

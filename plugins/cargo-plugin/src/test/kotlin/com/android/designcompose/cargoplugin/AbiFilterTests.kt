package com.android.designcompose.cargoplugin

import com.google.common.truth.Truth.assertThat
import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AbiFilterTests {
    private val defaultConfiguredAbis = setOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")

    @Test
    fun androidStudioInjectedTwoAbis() {
        assertThat(
            selectActiveAbis(defaultConfiguredAbis, "x86_64,arm64-v8a", null)
        ).containsExactly("x86_64")
    }

    @Test
    fun androidStudioInjectedOneAbi() {
        assertThat(
            selectActiveAbis(defaultConfiguredAbis, "arm64-v8a", null)
        ).containsExactly("arm64-v8a")

    }

    @Test
    fun androidStudioInjectedUnknownAbi() {
        assertThrows<GradleException> {
            selectActiveAbis(defaultConfiguredAbis, "unknown", null)
        }

    }

    @Test
    fun overrideOneAbi() {
        assertThat(
            selectActiveAbis(defaultConfiguredAbis, null, "x86_64")
        ).containsExactly("x86_64")
    }

    @Test
    fun overrideThreeAbis() {
        assertThat(
            selectActiveAbis(defaultConfiguredAbis, null, "x86,arm64-v8a,x86_64")
        ).containsExactly("x86", "arm64-v8a", "x86_64")
    }

    @Test
    fun overrideWithInvalid() {
        assertThrows<GradleException> {
            selectActiveAbis(defaultConfiguredAbis, null, "x86,unknown")
        }
    }
}
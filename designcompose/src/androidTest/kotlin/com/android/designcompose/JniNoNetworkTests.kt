package com.android.designcompose

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class JniNoNetworkTests {
    @Before
    fun setup() {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("svc wifi disable")
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("svc data disable")
    }
    @Test
    fun networkFailure() {
        assertFailsWith<ConnectionFailedException> {
            LiveUpdateJni.jniFetchDoc("DummyDocId", dummyFigmaTokenJson)
        }
    }
    @After
    fun teardown() {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("svc wifi enable")
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("svc data enable")
    }
}
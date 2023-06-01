package com.android.designcompose

import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.anyOf
import org.hamcrest.collection.IsIn.isOneOf
import org.hamcrest.core.IsInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
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
        val exception = assertFailsWith<IOException> {
            LiveUpdateJni.jniFetchDoc("DummyDocId", dummyFigmaTokenJson)
        }
        assertThat(exception, anyOf(IsInstanceOf(ConnectException::class.java),
            IsInstanceOf(SocketException::class.java)))
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
package com.android.designcompose

import com.android.designcompose.common.DocumentServerParams
import org.junit.Test
import kotlin.test.assertFailsWith

val dummyFigmaTokenJson = constructPostJson("NOT_A_FIGMA_TOKEN", null, DocumentServerParams())

class JniLiveWithoutTokenTests {
    /**
     * Invalid key test
     *
     * Tests that a fetch request using an invalid Figma API Key returns the proper failure
     */
    @Test
    fun invalidKey() {
        assertFailsWith(AccessDeniedException::class) {
            LiveUpdateJni.jniFetchDoc("DummyDocId", dummyFigmaTokenJson)
        }
    }
}
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

import com.android.designcompose.proto.Helloworld
import com.android.designcompose.proto.testMessage
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir

class BasicTests {

    @TempDir lateinit var tempDir: Path

    @Test
    fun testWriteAndRead() {
        val msg = testMessage { number = 5 }
        val msgFile = tempDir.resolve("test.msg")
        msgFile.outputStream().use { msg.writeTo(it) }

        val readMsg =
            msgFile.inputStream().use { Helloworld.TestMessage.newBuilder().mergeFrom(it).build() }
        assertEquals(5, readMsg.number)
    }
}

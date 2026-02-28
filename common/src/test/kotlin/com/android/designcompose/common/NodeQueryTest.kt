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

package com.android.designcompose.common

import kotlin.test.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NodeQueryTest {

    @Test
    fun testNodeIdToString() {
        val query = NodeQuery.id("2:101")
        assertEquals("id:2:101", query.encode())
    }

    @Test
    fun testNodeNameToString() {
        val query = NodeQuery.name("my_node")
        assertEquals("name:my_node", query.encode())
    }

    @Test
    fun testNodeVariantToString() {
        val query = NodeQuery.variant("variant_name", "parent_name")
        assertEquals("variant:variant_name\u001Fparent_name", query.encode())
    }

    @Test
    fun testNodeComponentSetToString() {
        val query = NodeQuery.componentSet("component_set_name")
        assertEquals("component_set:component_set_name", query.encode())
    }

    @Test
    fun testFromStringNodeId() {
        val query = NodeQuery.decode("id:2:101")
        assertEquals(NodeQuery.id("2:101"), query)
    }

    @Test
    fun testFromStringNodeName() {
        val query = NodeQuery.decode("name:my_node")
        assertEquals(NodeQuery.name("my_node"), query)
    }

    @Test
    fun testFromStringNodeNameUsesName() {
        val query = NodeQuery.decode("name:name:deadbeef")
        assertEquals(NodeQuery.name("name:deadbeef"), query)
    }

    @Test
    fun testFromStringNodeVariant() {
        val query = NodeQuery.decode("variant:variant_name\u001Fparent_name")
        assertEquals(NodeQuery.variant("variant_name", "parent_name"), query)
    }

    @Test
    fun testFromStringNodeComponentSet() {
        val query = NodeQuery.decode("component_set:component_set_name")
        assertEquals(NodeQuery.componentSet("component_set_name"), query)
    }

    @Test
    fun testFromStringInvalid() {
        assertThrows(IndexOutOfBoundsException::class.java) { NodeQuery.decode("invalid_query") }
    }

    @Test
    fun testEncodeDecode() {
        val queries =
            listOf(
                NodeQuery.NodeId("id1"),
                NodeQuery.NodeName("name1"),
                NodeQuery.NodeVariant("variant1", "parent1"),
                NodeQuery.NodeComponentSet("componentSet1"),
            )

        for (query in queries) {
            val encoded = query.encode()
            val decoded = NodeQuery.decode(encoded)
            assertEquals(query, decoded)
        }
    }
}

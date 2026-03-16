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

import com.android.designcompose.definition.DesignComposeDefinition
import com.android.designcompose.definition.view.View
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NodeQueryFunctionTest {
    @Test
    fun testConstructors() {
        val idQuery = NodeQuery.id("id1")
        assertThat(idQuery).isInstanceOf(NodeQuery.NodeId::class.java)
        assertThat((idQuery as NodeQuery.NodeId).id).isEqualTo("id1")

        val nameQuery = NodeQuery.name("name1")
        assertThat(nameQuery).isInstanceOf(NodeQuery.NodeName::class.java)
        assertThat((nameQuery as NodeQuery.NodeName).name).isEqualTo("name1")

        val variantQuery = NodeQuery.variant("variant1", "parent1")
        assertThat(variantQuery).isInstanceOf(NodeQuery.NodeVariant::class.java)
        assertThat((variantQuery as NodeQuery.NodeVariant).name).isEqualTo("variant1")
        assertThat((variantQuery as NodeQuery.NodeVariant).parent).isEqualTo("parent1")

        val componentSetQuery = NodeQuery.componentSet("componentSet1")
        assertThat(componentSetQuery).isInstanceOf(NodeQuery.NodeComponentSet::class.java)
        assertThat((componentSetQuery as NodeQuery.NodeComponentSet).name)
            .isEqualTo("componentSet1")
    }

    @Test
    fun testViews() {
        val view = View.newBuilder().build()
        val definition =
            DesignComposeDefinition.newBuilder()
                .putViews("id:id1", view)
                .putViews("name:name1", view)
                .build()

        val views = definition.views()
        assertThat(views).hasSize(2)
        assertThat(views[NodeQuery.NodeId("id1")]).isEqualTo(view)
        assertThat(views[NodeQuery.NodeName("name1")]).isEqualTo(view)
    }
}

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

import com.android.designcompose.serdegen.DesignComposeDefinition
import com.android.designcompose.serdegen.View

sealed class NodeQuery {
    data class NodeId(val id: String) : NodeQuery()

    data class NodeName(val name: String) : NodeQuery()

    data class NodeVariant(val name: String, val parent: String) : NodeQuery()

    data class NodeComponentSet(val name: String) : NodeQuery()

    companion object {
        fun id(id: String) = NodeId(id)

        fun name(name: String) = NodeName(name)

        fun variant(name: String, parent: String) = NodeVariant(name, parent)

        fun componentSet(name: String) = NodeComponentSet(name)

        fun decode(s: String): NodeQuery {
            val parts = s.split(":", limit = 2)
            val queryType = parts[0]
            val queryValue = parts[1]

            return when (queryType) {
                "id" -> NodeId(queryValue)
                "name" -> NodeName(queryValue)
                "variant" -> {
                    val variantParts = queryValue.split("\u001F")
                    if (variantParts.size != 2)
                        throw IllegalArgumentException("Invalid variant query string: $s")
                    NodeVariant(variantParts[0], variantParts[1])
                }
                "component_set" -> NodeComponentSet(queryValue)
                else -> throw IllegalArgumentException("Invalid query type: $queryType")
            }
        }
    }

    fun encode(): String {
        return when (this) {
            is NodeId -> "id:$id"
            is NodeName -> "name:$name"
            is NodeVariant -> "variant:${name}\u001F${parent}"
            is NodeComponentSet -> "component_set:$name"
        }
    }
}

fun DesignComposeDefinition.views(): Map<NodeQuery, View> {
    val views = mutableMapOf<NodeQuery, View>()
    for ((key, value) in this.views) {
        views[NodeQuery.decode(key)] = value
    }
    return views
}

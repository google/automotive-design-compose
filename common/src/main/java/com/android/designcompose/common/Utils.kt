/*
 * Copyright 2023 Google LLC
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

import com.android.designcompose.serdegen.View

// Given a variant name with comma separated properties in the form of property=variant, return the
// same node name with the properties sorted.
fun createSortedVariantName(nodeName: String): String {
    val props = nodeName.split(',')
    val sortedProperties: ArrayList<String> = ArrayList()
    for (p in props) {
        sortedProperties.add(p.trim())
    }
    sortedProperties.sort()
    return sortedProperties.joinToString(",")
}

// Given a node name of a variant e.g. "a=b, c=d, e=f", return a list of (property, value) pairs,
// e.g. [(a, b), (c, d), (e, f)]
fun nodeNameToPropertyValueList(nodeName: String): ArrayList<Pair<String, String>> {
    val propertyValueList = ArrayList<Pair<String, String>>()
    val propsAndValues = nodeName.split(',')
    for (p in propsAndValues) {
        val propAndVariant = p.split('=')
        if (propAndVariant.size == 2) {
            val propName = propAndVariant[0].trim()
            val variantName = propAndVariant[1].trim()
            propertyValueList.add(Pair(propName, variantName))
        }
    }
    return propertyValueList
}

class VariantPropertyMap {
    // HashMap of a single property name to a set of parent components that use that property name
    // property_name -> Set<parent_node_name>
    private val propertyToParentNodes: HashMap<String, HashSet<String>> = HashMap()

    // HashMap of variant properties in the format of:
    // parent_node_name -> [ property_name -> { set of property_name values } ] ]
    // Example:
    //   [#cluster/center-area] ->
    //     [#cluster/prnd] -> {P, *},
    //     [#cluster/charging] -> {off, on}
    private val propertyMap: HashMap<String, HashMap<String, HashSet<String>>> = HashMap()

    fun addProperty(parentNodeName: String, propertyName: String, variantName: String) {
        // Map the property name to a set of all parent component nodes that use the property
        val parentNodes = propertyToParentNodes[propertyName] ?: HashSet()
        parentNodes.add(parentNodeName)
        propertyToParentNodes[propertyName] = parentNodes

        // Map the parent component node to a hash of its property names to a set of all values
        // of that property
        val propertyToVariantValueMap = propertyMap[parentNodeName] ?: HashMap()
        val variantValuesSet = propertyToVariantValueMap[propertyName] ?: HashSet()
        variantValuesSet.add(variantName)
        propertyToVariantValueMap[propertyName] = variantValuesSet
        propertyMap[parentNodeName] = propertyToVariantValueMap
    }

    fun resolveVariantNameToView(nodeName: String, variantViewMap: HashMap<String, View>): View? {
        // Map this node's properties to their values
        val nodePropertyHash = HashMap<String, String>()
        val propertyValueList = nodeNameToPropertyValueList(nodeName)
        for (p in propertyValueList) {
            nodePropertyHash[p.first] = p.second
        }

        // Given all the properties used in this node, find parent components that use these
        // properties.
        val possibleParentNodes: ArrayList<HashSet<String>> = ArrayList()
        for (property in nodePropertyHash.keys) {
            val parentComponentNodes = propertyToParentNodes[property]
            if (parentComponentNodes != null) possibleParentNodes.add(parentComponentNodes)
        }

        // There can be multiple parent components that use these properties. We try and find the
        // one that uses the greatest number of these properties. If there are multiple parent
        // components that share the same number of properties, we just pick one. This typically
        // will not happen much, and only when a designer is experimenting with variants.
        val parentNodeCounts: HashMap<String, Int> = HashMap()
        for (set in possibleParentNodes) {
            for (name in set) {
                val count = (parentNodeCounts[name] ?: 0) + 1
                parentNodeCounts[name] = count
            }
        }
        var parentNodeToUse: String? = null
        var maxCount = 0
        var index = 0
        parentNodeCounts.forEach {
            if (it.value > maxCount) {
                maxCount = it.value
                parentNodeToUse = it.key
            }
            index += 1
        }

        // If we could not find any parent component, there's nothing else to do
        if (parentNodeToUse == null) return null

        // nodePropertyPossibleValues is the set of all possible variant names corresponding to
        // propertyName found in the Figma doc
        val nodePropertyPossibleValues = propertyMap[parentNodeToUse]

        if (nodePropertyHash.isEmpty() || nodePropertyPossibleValues?.isEmpty() != false)
            return null

        // If the parent component has extra properties that we don't need, add them to our hash
        // of properties so that when constructing the node name, it finds a valid node. This
        // method is imperfect -- it's possible that we could start with the node "a=b,c=d", but
        // find that the parent component in Figma has another property "e". This algorithm just
        // selects a random value of e, so it could come up with the node name "a=b,c=d,e=f" when
        // that node name is invalid and only "a=b,c=d,e=g" is valid. However, again this typically
        // will not happen much, only when a designer is experimenting with variants
        if (nodePropertyPossibleValues.size > nodePropertyHash.size) {
            nodePropertyPossibleValues.forEach {
                if (!nodePropertyHash.containsKey(it.key)) {
                    nodePropertyHash[it.key] = it.value.first()
                }
            }
        }

        // First try to use the exact values from the node name and retrieve that view.
        val resolvedNameProperties: ArrayList<String> = ArrayList()
        nodePropertyHash.forEach {
            val propertyName = it.key
            val variantName = it.value
            val possibleValues = nodePropertyPossibleValues[propertyName]
            if (possibleValues != null) {
                when {
                    possibleValues.contains(variantName) ->
                        resolvedNameProperties.add("$propertyName=$variantName")
                    possibleValues.contains("*") -> resolvedNameProperties.add("$propertyName=*")
                    else -> return null
                }
            } else {
                return null
            }
        }

        var view = getViewFromPropertyList(resolvedNameProperties, variantViewMap)
        if (view != null) return view

        // If we haven't found a view, try all all combinations of property to variant values,
        // using the '*' version of properties if they exist. This produces up to O(n^2)
        // possibilities
        // if all properties have a '*' version, but n is typically small (2 or 3).
        val nodePropertyList = nodePropertyHash.keys.toList()
        resolvedNameProperties.clear()
        view =
            findViewFromPossibleNodeNames(
                nodePropertyList,
                nodePropertyHash,
                nodePropertyPossibleValues,
                variantViewMap,
                resolvedNameProperties,
                0
            )
        if (view != null) return view

        return null
    }

    private fun findViewFromPossibleNodeNames(
        nodePropertyList: List<String>,
        nodePropertyHash: HashMap<String, String>,
        nodePropertyPossibleValues: HashMap<String, HashSet<String>>,
        variantViewMap: HashMap<String, View>,
        resolvedNameProperties: ArrayList<String>,
        index: Int
    ): View? {
        val propertyName = nodePropertyList[index]
        val variantName = nodePropertyHash[propertyName]!!
        val possibleValues = nodePropertyPossibleValues[propertyName]!!

        if (possibleValues.contains(variantName)) {
            resolvedNameProperties.add("$propertyName=$variantName")
            if (index == nodePropertyList.size - 1) {
                val view = getViewFromPropertyList(resolvedNameProperties, variantViewMap)
                if (view != null) return view
            } else {
                val view =
                    findViewFromPossibleNodeNames(
                        nodePropertyList,
                        nodePropertyHash,
                        nodePropertyPossibleValues,
                        variantViewMap,
                        resolvedNameProperties,
                        index + 1
                    )
                if (view != null) return view
            }
            resolvedNameProperties.removeLast()
        }
        if (possibleValues.contains("*")) {
            resolvedNameProperties.add("$propertyName=*")
            if (index == nodePropertyList.size - 1) {
                val view = getViewFromPropertyList(resolvedNameProperties, variantViewMap)
                if (view != null) return view
            } else {
                val view =
                    findViewFromPossibleNodeNames(
                        nodePropertyList,
                        nodePropertyHash,
                        nodePropertyPossibleValues,
                        variantViewMap,
                        resolvedNameProperties,
                        index + 1
                    )
                if (view != null) return view
            }
            resolvedNameProperties.removeLast()
        }
        return null
    }

    private fun getViewFromPropertyList(
        resolvedNameProperties: ArrayList<String>,
        variantViewMap: HashMap<String, View>
    ): View? {
        val propertiesClone = ArrayList(resolvedNameProperties)
        propertiesClone.sort()
        val resolvedNodeName = propertiesClone.joinToString(",")
        return variantViewMap[resolvedNodeName]
    }
}

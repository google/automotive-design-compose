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

package com.android.designcompose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.android.designcompose.common.DesignDocId
import com.android.designcompose.definition.element.ColorOrVar
import com.android.designcompose.definition.element.NumOrVar
import com.android.designcompose.definition.element.Variable
import com.android.designcompose.definition.element.VariableMap
import com.android.designcompose.definition.element.VariableValue
import com.android.designcompose.utils.toColor

// A variable mode, e.g. "light" or "dark"
typealias VariableMode = String

// A variable collection, e.g. "material-theme" or "my-theme"
typealias VarCollectionName = String

// A mapping of collection names to the current mode for that collection
typealias VariableModeValues = HashMap<VarCollectionName, VariableMode>

// Current variable collection to use
private val LocalVariableCollection = compositionLocalOf<VarCollectionName?> { null }

// Current set of variable modes to use, if overridden in code
private val LocalVariableModeValuesOverride = compositionLocalOf<VariableModeValues?> { null }

// Current set of variable modes to use, as retrieved from the design source
private val LocalVariableModeValuesDoc = compositionLocalOf<VariableModeValues> { hashMapOf() }

// Current boolean value to represent whether we should attempt to override the design specified
// theme with the MaterialTheme
private val LocalVariableUseMaterialTheme = compositionLocalOf { false }

// An accessor class to retrieve the current values of various variable states
object LocalVariableState {
    val collection: VarCollectionName?
        @Composable @ReadOnlyComposable get() = LocalVariableCollection.current

    val modeValues: VariableModeValues?
        @Composable @ReadOnlyComposable get() = LocalVariableModeValuesOverride.current

    val useMaterialTheme: Boolean
        @Composable @ReadOnlyComposable get() = LocalVariableUseMaterialTheme.current

    @Composable
    internal fun hasOverrideModeValues(): Boolean {
        return modeValues != null
    }
}

// This class takes a snapshot of the current state of all variable values, including the current
// collection, modes, and material theme values. Since the current values must be retrieved from
// a composable function but is needed by non-composable functions, we construct this snapshot
// and pass it to the non-composable functions
internal class VariableState(
    val varCollection: VarCollectionName? = null,
    val varModeValues: VariableModeValues? = null,
    val useMaterialTheme: Boolean = false,
    val materialColorScheme: ColorScheme? = null,
    val materialTypography: Typography? = null,
    val materialShapes: Shapes? = null,
) {
    companion object {
        @Composable
        fun create(updatedModeValues: VariableModeValues? = null): VariableState {
            return VariableState(
                varCollection = LocalVariableState.collection,
                varModeValues = updatedModeValues ?: LocalVariableState.modeValues,
                useMaterialTheme = LocalVariableState.useMaterialTheme,
                materialColorScheme = MaterialTheme.colorScheme,
                materialTypography = MaterialTheme.typography,
                materialShapes = MaterialTheme.shapes,
            )
        }
    }

    // Create a copy of this VariableState, replacing any explicitly set mode values with those
    // passed in. Return the new copy.
    fun copyWithModeValues(explicitModeValues: MutableMap<String, String>): VariableState {
        val newModeValues =
            this.varModeValues?.let { VariableModeValues(it) } ?: VariableModeValues()
        // The mode values passed in are in the format of collection ID -> mode ID. Convert this to
        // collection name -> mode name.
        explicitModeValues.forEach { (collectionId, modeId) ->
            val collection = VariableManager.getCollection(collectionId)
            collection?.let { c ->
                val mode = c.modeIdHashMap[modeId]
                mode?.let { m -> newModeValues[c.name] = m.name }
            }
        }
        return VariableState(
            this.varCollection,
            newModeValues,
            this.useMaterialTheme,
            this.materialColorScheme,
            this.materialTypography,
            this.materialShapes,
        )
    }
}

// Declare a CompositionLocal object of the specified variable collection. If null, no collection is
// set and whatever collection variables refer to will be used
@Composable
fun DesignVariableCollection(collection: VarCollectionName?, content: @Composable () -> Unit) =
    if (collection != null)
        CompositionLocalProvider(LocalVariableCollection provides collection) { content() }
    else content()

// Declare a CompositionLocal object of the specified mode values. If null, no mode is set and
// whatever mode values are set in the node are used, or the default mode of the collection.
@Composable
fun DesignVariableModeValues(modeValues: VariableModeValues?, content: @Composable () -> Unit) =
    if (modeValues != null)
        CompositionLocalProvider(LocalVariableModeValuesOverride provides modeValues) { content() }
    else content()

// Declare a CompositionLocal object of the specified mode values explicitly set from a view. If
// null, no mode is set and whatever mode values are set in the node are used, or the default mode
// of the collection.
@Composable
internal fun DesignVariableExplicitModeValues(
    modeValues: VariableModeValues?,
    content: @Composable () -> Unit,
) =
    if (modeValues != null)
        CompositionLocalProvider(LocalVariableModeValuesDoc provides modeValues) { content() }
    else content()

// Declare a CompositionLocal object to specify whether the current MaterialTheme should be looked
// up and used in place of variable values, if the variables are MaterialTheme variables.
@Composable
fun DesignMaterialThemeProvider(useMaterialTheme: Boolean = true, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalVariableUseMaterialTheme provides useMaterialTheme) { content() }

// VariableManager holds the VariableMap retrieved from the design file and has functions to find
// the value of a variable from the variable ID.
internal object VariableManager {
    // Since we can have variables from multiple documents, store a variable map for each document
    private val docVarMap: HashMap<String, VariableMap> = HashMap()
    // A global variable map containing entries from all documents. We currently don't support
    // duplicate variable names across multiple documents.
    private var varMap: VariableMap = VariableMap.getDefaultInstance()

    private lateinit var currentDocId: DesignDocId

    internal fun init(docId: DesignDocId, map: VariableMap) {

        // Remove old entries for docId
        val oldVarMap = docVarMap[docId.id]
        oldVarMap?.collectionsMap?.forEach { varMap.collectionsMap.remove(it.key) }
        oldVarMap?.collectionNameMapMap?.forEach { varMap.collectionNameMapMap.remove(it.key) }
        oldVarMap?.variablesMap?.forEach { varMap.variablesMap.remove(it.key) }
        oldVarMap?.variableNameMapMap?.forEach { varMap.variableNameMapMap.remove(it.key) }

        // Add new entries for docId
        docVarMap[docId.id] = map
        varMap.collectionsMap.putAll(map.collectionsMap)
        varMap.collectionNameMapMap.putAll(map.collectionNameMapMap)
        varMap.variablesMap.putAll(map.variablesMap)
        varMap.variableNameMapMap.putAll(map.variableNameMapMap)

        currentDocId = docId
    }

    // If the developer has not explicitly set variable override values, check to see if any
    // variable modes have been set on this node. If so, return the modeValues set.
    @Composable
    internal fun currentModeValues(
        explicitVariableModes: MutableMap<String, String>
    ): VariableModeValues? {
        var modeValues: VariableModeValues? = null
        if (!LocalVariableState.hasOverrideModeValues()) {
            if (explicitVariableModes.isNotEmpty()) {
                val modes = explicitVariableModes
                modeValues = updateVariableStateFromModeValues(modes)
            } else {
                modeValues = LocalVariableModeValuesDoc.current
            }
        }
        return modeValues
    }

    // Given a set of explicitly set mode values on a node, create a VariableModeValues hash
    // combining the current mode values with the values passed in.
    @Composable
    internal fun updateVariableStateFromModeValues(
        modeValues: MutableMap<String, String>
    ): VariableModeValues {
        val newModeValues = VariableModeValues(LocalVariableModeValuesDoc.current)
        modeValues.forEach { (collectionId, modeId) ->
            val collection = varMap.collectionsMap[collectionId]
            collection?.let { c ->
                val mode = c.modeIdHashMap[modeId]
                mode?.let { m -> newModeValues[c.name] = m.name }
            }
        }
        return newModeValues
    }

    // Return the collection given the collection ID
    internal fun getCollection(
        collectionId: String
    ): com.android.designcompose.definition.element.Collection? {
        return varMap.collectionsMap[collectionId]
    }

    // Given a variable ID, return the color associated with it
    internal fun getColor(varId: String, fallback: Color?, variableState: VariableState): Color? {
        // Resolve varId into a variable. If a different collection has been set, this will return
        // a variable of the same name from the override collection.
        val variable = resolveVariable(varId, variableState)
        variable?.let { v ->
            return v.getColor(varMap, variableState)
        }
        Feedback.documentVariableMissingWarning(currentDocId, varId)
        return fallback
    }

    // Given a variable ID, return the number associated with it
    internal fun getNumber(varId: String, fallback: Float?, variableState: VariableState): Float? {
        val variable = resolveVariable(varId, variableState)
        variable?.let { v ->
            return v.getNumber(varMap, variableState)
        }
        Feedback.documentVariableMissingWarning(currentDocId, varId)
        return fallback
    }

    // Given a variable ID, return a Variable if it can be found. If an override collection has been
    // set, this will return a variable from that collection if one of the same name exists.
    // Otherwise, this will return the variable with the given ID.
    private fun resolveVariable(varId: String, variableState: VariableState): Variable? {
        val variable = varMap.variablesMap[varId]
        variable?.let { v ->
            // If using material theme, return the variable since we don't need to resolve it based
            // on an overridden collection
            if (variableState.useMaterialTheme) return v
            val collectionOverride = variableState.varCollection
            collectionOverride?.let { cName ->
                val collectionId = varMap.collectionNameMapMap[cName]
                collectionId?.let { cId ->
                    val nameMap = varMap.variableNameMapMap[cId]
                    nameMap?.let { nMap ->
                        val resolvedVarId = nMap.m[v.name]
                        resolvedVarId?.let { newVarId ->
                            return varMap.variablesMap[newVarId]
                        }
                    }
                }
            }
            return v
        }
        return null
    }

    // Retrieve the VariableValue from this variable given the current variable state
    private fun Variable.getValue(
        variableMap: VariableMap,
        variableState: VariableState,
    ): VariableValue? {
        val collection = variableMap.collectionsMap[variableCollectionId]
        collection?.let { c ->
            val modeName = variableState.varModeValues?.get(c.name)
            val modeId =
                modeName?.let { mName -> c.modeNameHashMap[mName] }
                    ?: c.modeIdHashMap[c.defaultModeId]
                        ?.id // Fallback to default mode in collection
            modeId?.let { mId ->
                return valuesByMode.valuesByModeMap[mId]
            }
        }
        return null
    }

    // Return this variable's color given the current variable state.
    private fun Variable.getColor(variableMap: VariableMap, variableState: VariableState): Color? {
        // Use the material theme override if one exists
        MaterialThemeValues.getColor(name, variableCollectionId, variableState)?.let {
            return it
        }
        val value = getValue(variableMap, variableState)
        return when (value?.valueCase) {
            VariableValue.ValueCase.COLOR -> value.color.toColor()
            VariableValue.ValueCase.ALIAS ->
                resolveVariable(value.alias, variableState)?.getColor(variableMap, variableState)

            else -> null
        }
    }

    // Return this variable's number given the current variable state.
    private fun Variable.getNumber(variableMap: VariableMap, variableState: VariableState): Float? {
        val value = getValue(variableMap, variableState)
        return when (value?.valueCase) {
            VariableValue.ValueCase.NUMBER -> value.number
            VariableValue.ValueCase.ALIAS ->
                resolveVariable(value.alias, variableState)?.getNumber(variableMap, variableState)

            else -> null
        }
    }
}

// Return the value out of a NumOrVar enum.
internal fun NumOrVar.getValue(variableState: VariableState): Float {
    return when (this.numOrVarTypeCase) {
        NumOrVar.NumOrVarTypeCase.NUM -> num
        NumOrVar.NumOrVarTypeCase.VAR ->
            VariableManager.getNumber(`var`.id, `var`.fallback, variableState) ?: 0F
        else -> 0F
    }
}

// Return the value of a ColorOrVar enum
internal fun ColorOrVar.getValue(variableState: VariableState): Color? {
    return when (this.colorOrVarTypeCase) {
        ColorOrVar.ColorOrVarTypeCase.COLOR -> color.toColor()
        ColorOrVar.ColorOrVarTypeCase.VAR -> {
            val fallback = `var`.fallback.toColor()
            VariableManager.getColor(`var`.id, fallback, variableState)
        }
        else -> null
    }
}

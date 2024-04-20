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
import com.android.designcompose.serdegen.ColorOrVar
import com.android.designcompose.serdegen.NumOrVar
import com.android.designcompose.serdegen.Variable
import com.android.designcompose.serdegen.VariableMap
import com.android.designcompose.serdegen.VariableValue

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
    private var varMap: VariableMap = VariableMap(HashMap(), HashMap(), HashMap(), HashMap())

    internal fun init(docId: String, map: VariableMap) {
        // Remove old entries for docId
        val oldVarMap = docVarMap[docId]
        oldVarMap?.collections?.forEach { varMap.collections.remove(it.key) }
        oldVarMap?.collection_name_map?.forEach { varMap.collection_name_map.remove(it.key) }
        oldVarMap?.variables?.forEach { varMap.variables.remove(it.key) }
        oldVarMap?.variable_name_map?.forEach { varMap.variable_name_map.remove(it.key) }

        // Add new entries for docId
        docVarMap[docId] = map
        varMap.collections.putAll(map.collections)
        varMap.collection_name_map.putAll(map.collection_name_map)
        varMap.variables.putAll(map.variables)
        varMap.variable_name_map.putAll(map.variable_name_map)
    }

    // Given a set of explicitly set mode values on a node, create a VariableModeValues hash
    // combining the current mode values with the values passed in.
    @Composable
    internal fun updateVariableStateFromModeValues(
        modeValues: MutableMap<String, String>
    ): VariableModeValues {
        val newModeValues = VariableModeValues(LocalVariableModeValuesDoc.current)
        modeValues.forEach { (collectionId, modeId) ->
            val collection = varMap.collections[collectionId]
            collection?.let { c ->
                val mode = c.mode_id_hash[modeId]
                mode?.let { m -> newModeValues[c.name] = m.name }
            }
        }
        return newModeValues
    }

    // Given a variable ID, return the color associated with it
    internal fun getColor(varId: String, variableState: VariableState): Color? {
        // Resolve varId into a variable. If a different collection has been set, this will return
        // a variable of the same name from the override collection.
        val variable = resolveVariable(varId, variableState)
        variable?.let { v ->
            return v.getColor(varMap, variableState)
        }
        return null
    }

    // Given a variable ID, return the number associated with it
    internal fun getNumber(varId: String, variableState: VariableState): Float? {
        val variable = resolveVariable(varId, variableState)
        variable?.let { v ->
            return v.getNumber(varMap, variableState)
        }
        return null
    }

    // Given a variable ID, return a Variable if it can be found. If an override collection has been
    // set, this will return a variable from that collection if one of the same name exists.
    // Otherwise, this will return the variable with the given ID.
    private fun resolveVariable(varId: String, variableState: VariableState): Variable? {
        val variable = varMap.variables[varId]
        variable?.let { v ->
            // If using material theme, return the variable since we don't need to resolve it based
            // on an overridden collection
            if (variableState.useMaterialTheme) return v
            val collectionOverride = variableState.varCollection
            collectionOverride?.let { cName ->
                val collectionId = varMap.collection_name_map[cName]
                collectionId?.let { cId ->
                    val nameMap = varMap.variable_name_map[cId]
                    nameMap?.let { nMap ->
                        val resolvedVarId = nMap[v.name]
                        resolvedVarId?.let { newVarId ->
                            return varMap.variables[newVarId]
                        }
                    }
                }
            }
            return v
        }
        return null
    }

    // Return this variable's color given the current variable state.
    private fun Variable.getColor(
        variableMap: VariableMap,
        variableState: VariableState,
    ): Color? {
        val collection = variableMap.collections[variable_collection_id]
        collection?.let { c ->
            // Use the material theme override if one exists
            val materialColor = MaterialThemeValues.getColor(name, c.name, variableState)
            materialColor?.let {
                return it
            }

            val modeName = variableState.varModeValues?.get(c.name)
            val modeId =
                modeName?.let { mName -> c.mode_name_hash[mName] }
                    ?: c.mode_id_hash[c.default_mode_id]
                        ?.id // Fallback to default mode in collection
            modeId?.let { mId ->
                val value = values_by_mode.values_by_mode[mId]
                value?.let { vv ->
                    when (vv) {
                        is VariableValue.Color -> return vv.value.toColor()
                        is VariableValue.Alias ->
                            return resolveVariable(vv.value.id, variableState)
                                ?.getColor(variableMap, variableState)
                        else -> return null
                    }
                }
            }
        }
        return null
    }

    // Return this variable's number given the current variable state.
    private fun Variable.getNumber(
        variableMap: VariableMap,
        variableState: VariableState,
    ): Float? {
        val collection = variableMap.collections[variable_collection_id]
        collection?.let { c ->
            variableState.varModeValues?.let { modes ->
                val modeName = modes[c.name]
                val modeId =
                    modeName?.let { mName -> c.mode_name_hash[mName] }
                        ?: c.mode_id_hash[
                                c.default_mode_id] // Fallback to default mode in collection
                modeId?.let { mId ->
                    val value = values_by_mode.values_by_mode[mId]
                    value?.let { vv ->
                        when (vv) {
                            is VariableValue.Number -> return vv.value
                            is VariableValue.Alias ->
                                return resolveVariable(vv.value.id, variableState)
                                    ?.getNumber(variableMap, variableState)
                            else -> return null
                        }
                    }
                }
            }
        }
        return null
    }
}

// Return the value out of a NumOrVar enum.
internal fun NumOrVar.getValue(variableState: VariableState): Float {
    return when (this) {
        is NumOrVar.Num -> value
        is NumOrVar.Var -> VariableManager.getNumber(value, variableState) ?: 0F
        else -> 0F
    }
}

// Return the value of a ColorOrVar enum
internal fun ColorOrVar.getValue(variableState: VariableState): Color? {
    return when (this) {
        is ColorOrVar.Color -> value.toColor()
        is ColorOrVar.Var -> VariableManager.getColor(value, variableState)
        else -> null
    }
}

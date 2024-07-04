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

package com.android.designcompose.codegen

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.OutputStream

// Helper class that keeps track of query node names and ignored image node names from properties of
// @DesignModuleClass classes. This class stores hashes of these node names indexed by class name
// so that @DesignComponent functions that use @DesignModule objects can generate the proper
// queries() and ignoredImages() functions by querying this class.
class ModuleNodeNameTable {
    private val queries: HashMap<String, HashSet<String>> = HashMap()
    private val ignoredImages: HashMap<String, HashSet<String>> = HashMap()
    private val jsonContentArrays: HashMap<String, JsonArray> = HashMap()
    // Maps a module class name to a set of module class names that it references.
    private val nestedModules: HashMap<String, HashSet<String>> = HashMap()

    // Return a set of node queries for the module named className. This includes any queries from
    // nested modules as well.
    fun getNodeQueries(className: String): HashSet<String> {
        val allQueries = HashSet<String>()
        // Call function to recursively add queries
        addModuleNodeQueries(className, allQueries)
        return allQueries
    }

    // Add node queries associated with className, then recurse on any classes that className
    // references in its properties.
    private fun addModuleNodeQueries(className: String, allQueries: HashSet<String>) {
        // Add queries for the specified class
        val classQueries = queries[className]
        classQueries?.let { allQueries.addAll(it) }
        // Recurse on any modules that className references
        nestedModules[className]?.forEach { addModuleNodeQueries(it, allQueries) }
    }

    // Return a set of ignored image node names for the module named className. This includes any
    // node names from nested modules as well.
    fun getIgnoredImages(className: String): HashSet<String> {
        val allIgnored = HashSet<String>()
        // Call function to recursively add ignored image nodes
        addModuleIgnoredImages(className, allIgnored)
        return allIgnored
    }

    // Add ignored node names associated with className, then recurse on any classes that className
    // references in its properties.
    private fun addModuleIgnoredImages(className: String, allIgnored: HashSet<String>) {
        // Add ignored images for the specified class
        val classIgnored = ignoredImages[className]
        classIgnored?.let { allIgnored.addAll(it) }
        // Recurse on any modules that className references
        nestedModules[className]?.forEach { addModuleIgnoredImages(it, allIgnored) }
    }

    // Return a JsonArray of all the customizations for the module named className. This includes
    // any customizations from nested modules as well.
    fun getJsonContentArray(className: String): JsonArray {
        val combinedArrays = JsonArray()
        // Call function to recursively add json content arrays
        addModuleJsonContentArray(className, combinedArrays)
        return combinedArrays
    }

    // Add customizations into combinedArrays, then recurse on any classes that className references
    // in its properties
    private fun addModuleJsonContentArray(className: String, combinedArrays: JsonArray) {
        // Add json content array for the specified class
        val jsonContentArray = jsonContentArrays[className]
        jsonContentArray?.let { combinedArrays.addAll(it) }
        // Recurse on any modules that className references
        nestedModules[className]?.forEach { addModuleJsonContentArray(it, combinedArrays) }
    }

    internal fun addNodeToQueries(className: String, nodeName: String) {
        if (!queries.containsKey(className)) queries[className] = HashSet()
        queries[className]?.add(nodeName)
    }

    internal fun addIgnoredImage(className: String, nodeName: String) {
        if (!ignoredImages.containsKey(className)) ignoredImages[className] = HashSet()
        ignoredImages[className]?.add(nodeName)
    }

    internal fun addJsonContentArray(className: String, jsonContentArray: JsonArray) {
        jsonContentArrays[className] = jsonContentArray
    }

    internal fun addNestedModule(className: String, moduleName: String, logger: KSPLogger) {
        if (!nestedModules.containsKey(className)) nestedModules[className] = HashSet()

        // Check that there is no recursive nesting of module classes
        if (hasRecursiveNesting(className, moduleName)) {
            logger.error("Recursive nesting detected between $className, $moduleName")
            return
        }

        nestedModules[className]?.add(moduleName)
    }

    private fun hasRecursiveNesting(className: String, moduleName: String): Boolean {
        if (className == moduleName) return true
        val nested = nestedModules[moduleName]
        nested?.forEach { if (hasRecursiveNesting(className, it)) return true }
        return false
    }
}

// For each @DesignModuleClass annotation, create a DesignModuleVisitor class to process the
// annotation and associated class.
internal fun processDesignModulesClasses(
    resolver: Resolver,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
): ModuleNodeNameTable {
    val nodeNameTable = ModuleNodeNameTable()

    val moduleClasses =
        resolver
            .getSymbolsWithAnnotation("com.android.designcompose.annotation.DesignModuleClass")
            .filterIsInstance<KSClassDeclaration>() // Making sure we take only class declarations.
    moduleClasses.forEach {
        it.accept(
            DesignModuleVisitor(
                codeGenerator,
                logger,
                nodeNameTable,
            ),
            Unit
        )
    }
    return nodeNameTable
}

// This class generates a file for a @DesignModuleClass class and creates an extension function
// for the class that sets up all the customizations used in the class.
private class DesignModuleVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val nodeNameTable: ModuleNodeNameTable,
) : KSVisitorVoid() {
    private val jsonCustomizations: JsonArray = JsonArray()
    private lateinit var out: OutputStream
    private lateinit var className: String
    private lateinit var qualifiedclassName: String

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        className = classDeclaration.simpleName.asString()
        qualifiedclassName = classDeclaration.qualifiedName?.asString() ?: return
        val packageName = classDeclaration.packageName.asString()

        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.error(
                "@DesignModuleClass must be used with a class. ${classDeclaration.classKind.type} not supported"
            )
            return
        }

        // Create a new file for each @DesignDoc annotation
        out =
            createNewFile(
                codeGenerator,
                className,
                packageName,
                setOf(classDeclaration.containingFile!!),
            )

        // Create the customizations() extension function and boilerplate code
        out += "fun $className.customizations(): CustomizationContext {\n"
        out += "    val customizations = CustomizationContext()\n"
        out += "    val variantProperties = HashMap<String, String>()\n"

        // Iterate through the properties and generate code to set a customization for each
        val properties = classDeclaration.declarations
        properties.forEach { property -> property.accept(this, data) }

        out += "    customizations.setVariantProperties(variantProperties)\n"
        out += "    return customizations\n"
        out += "}\n"

        out.close()
        nodeNameTable.addJsonContentArray(qualifiedclassName, jsonCustomizations)
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
        // Skip non-annotated properties
        if (property.annotations.asIterable().toList().isEmpty()) return

        val propertyName = property.simpleName.asString()

        // Get the node name in the Design annotation or property name in the DesignVariant
        // annotation.
        val nodeName = property.getAnnotatedNodeName() ?: property.getVariantProperty()

        // Add a customization for this arg to the json list
        if (nodeName != null) {
            val jsonHash = JsonObject()
            val paramType = property.customizationType()
            jsonHash.addProperty("name", propertyName)
            jsonHash.addProperty("node", nodeName)
            jsonHash.addProperty("kind", paramType.name)

            val jsonContentArray = property.buildDesignContentTypesJson(propertyName, logger)
            jsonContentArray?.let { jsonHash.add("content", it) }

            val previewContentArray = property.buildDesignPreviewContentPropertyJson()
            if (!previewContentArray.isEmpty) jsonHash.add("previewContent", previewContentArray)

            val enumValues = property.type.buildEnumValuesJson()
            enumValues?.let { jsonHash.add("values", enumValues) }
            jsonCustomizations.add(jsonHash)

            val variantProperty = property.getVariantProperty()
            variantProperty?.let {
                out += "    variantProperties[\"$variantProperty\"] = $propertyName.name\n"
                // Add any variants encountered to the set of node queries
                nodeNameTable.addNodeToQueries(qualifiedclassName, it)
                return
            }
        }

        val customizationType = property.customizationType()

        // For modules, add a nested module reference from this module to the property.
        // Output a line to merge the customizations into ours
        if (customizationType == CustomizationType.Module) {
            property.type.resolve().declaration.qualifiedName?.let {
                nodeNameTable.addNestedModule(qualifiedclassName, it.asString(), logger)
            }
            out += "    customizations.mergeFrom($propertyName.customizations())\n"
            return
        }

        // If at this point nodeName is null, do nothing and return
        nodeName ?: return

        // Add to set of ignored images
        if (property.customizationType().shouldIgnoreImage())
            nodeNameTable.addIgnoredImage(qualifiedclassName, nodeName)

        // Create a line of code to set the customization based on the customization type
        when (customizationType) {
            CustomizationType.Text ->
                out += "    customizations.setText(\"$nodeName\", $propertyName)\n"
            CustomizationType.TextState ->
                out += "    customizations.setTextState(\"$nodeName\", $propertyName)\n"
            CustomizationType.Image ->
                out += "    customizations.setImage(\"$nodeName\", $propertyName)\n"
            CustomizationType.Brush ->
                out += "    customizations.setBrush(\"$nodeName\", $propertyName)\n"
            CustomizationType.BrushFunction ->
                out += "    customizations.setBrushFunction(\"$nodeName\", $propertyName)\n"
            CustomizationType.Modifier ->
                out += "    customizations.setModifier(\"$nodeName\", $propertyName)\n"
            CustomizationType.TapCallback ->
                out += "    customizations.setTapCallback(\"$nodeName\", $propertyName)\n"
            CustomizationType.ContentReplacement ->
                out += "    customizations.setContent(\"$nodeName\", $propertyName)\n"
            CustomizationType.ComponentReplacement ->
                out += "    customizations.setComponent(\"$nodeName\", $propertyName)\n"
            CustomizationType.ListContent ->
                out += "    customizations.setListContent(\"$nodeName\", $propertyName)\n"
            CustomizationType.ImageWithContext ->
                out += "    customizations.setImageWithContext(\"$nodeName\", $propertyName)\n"
            CustomizationType.Visibility ->
                out += "    customizations.setVisible(\"$nodeName\", $propertyName)\n"
            CustomizationType.VisibilityState ->
                out += "    customizations.setVisibilityState(\"$nodeName\", $propertyName)\n"
            CustomizationType.TextStyle ->
                out += "    customizations.setTextStyle(\"$nodeName\", $propertyName)\n"
            CustomizationType.Meter ->
                out += "    customizations.setMeterValue(\"$nodeName\", $propertyName)\n"
            CustomizationType.MeterFunction ->
                out += "    customizations.setMeterFunction(\"$nodeName\", $propertyName)\n"
            else ->
                logger.error(
                    "Invalid parameter type ${property.type.typeString()} property \"$propertyName\""
                )
        }
    }
}

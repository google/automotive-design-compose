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

import com.android.designcompose.annotation.DesignMetaKey
import com.android.designcompose.annotation.DesignPreviewContent
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.symbol.KSDynamicReference
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSModifierListOwner
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSParenthesizedReference
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.OutputStream
import java.util.Vector
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

class BuilderProcessor(private val codeGenerator: CodeGenerator, val logger: KSPLogger) :
    SymbolProcessor {
    operator fun OutputStream.plusAssign(str: String) {
        this.write(str.toByteArray())
    }

    fun createNewFile(
        className: String,
        packageName: String,
        dependencies: Set<KSFile>
    ): OutputStream {
        val fileName = className.replace('.', '_') + "_gen"
        val file =
            codeGenerator.createNewFile(
                dependencies = Dependencies(false, *dependencies.toTypedArray()),
                packageName = packageName,
                fileName = fileName
            )
        file += "package $packageName\n\n"
        file += "import androidx.compose.runtime.Composable\n"
        file += "import androidx.compose.ui.text.TextStyle\n"
        file += "import android.graphics.Bitmap\n"
        file += "import androidx.compose.ui.graphics.Brush\n"
        file += "import androidx.compose.ui.Modifier\n"
        file += "import androidx.compose.ui.semantics.semantics\n"
        file += "import androidx.compose.runtime.mutableStateOf\n"
        file += "import androidx.compose.runtime.remember\n"
        file += "import androidx.compose.ui.platform.ComposeView\n"
        file += "import androidx.compose.runtime.CompositionLocalProvider\n"
        file += "import androidx.compose.runtime.compositionLocalOf\n"
        file += "import android.widget.FrameLayout\n"
        file += "import android.util.DisplayMetrics\n"
        file += "import android.app.Activity\n"
        file += "import android.view.ViewGroup\n"
        file += "import android.os.Build\n"

        file += "import com.android.designcompose.annotation.DesignMetaKey\n"
        file += "import com.android.designcompose.serdegen.NodeQuery\n"
        file += "import com.android.designcompose.common.DocumentServerParams\n"
        file += "import com.android.designcompose.ComponentReplacementContext\n"
        file += "import com.android.designcompose.ImageReplacementContext\n"
        file += "import com.android.designcompose.CustomizationContext\n"
        file += "import com.android.designcompose.DesignDoc\n"
        file += "import com.android.designcompose.DesignComposeCallbacks\n"
        file += "import com.android.designcompose.DesignDocSettings\n"
        file += "import com.android.designcompose.DesignSwitcherPolicy\n"
        file += "import com.android.designcompose.OpenLinkCallback\n"
        file += "import com.android.designcompose.DesignNodeData\n"
        file += "import com.android.designcompose.DesignInjectKey\n"
        file += "import com.android.designcompose.ListContent\n"
        file += "import com.android.designcompose.setKey\n"
        file += "import com.android.designcompose.mergeFrom\n"
        file += "import com.android.designcompose.setComponent\n"
        file += "import com.android.designcompose.setContent\n"
        file += "import com.android.designcompose.setListContent\n"
        file += "import com.android.designcompose.setCustomComposable\n"
        file += "import com.android.designcompose.setImage\n"
        file += "import com.android.designcompose.setImageWithContext\n"
        file += "import com.android.designcompose.setBrush\n"
        file += "import com.android.designcompose.setBrushFunction\n"
        file += "import com.android.designcompose.setMeterValue\n"
        file += "import com.android.designcompose.setMeterFunction\n"
        file += "import com.android.designcompose.setModifier\n"
        file += "import com.android.designcompose.setTapCallback\n"
        file += "import com.android.designcompose.setOpenLinkCallback\n"
        file += "import com.android.designcompose.setText\n"
        file += "import com.android.designcompose.setTextFunction\n"
        file += "import com.android.designcompose.setVariantProperties\n"
        file += "import com.android.designcompose.setVisible\n"
        file += "import com.android.designcompose.TapCallback\n"
        file += "import com.android.designcompose.ParentComponentInfo\n"
        file += "import com.android.designcompose.sDocClass\n"
        file += "import com.android.designcompose.LocalCustomizationContext\n\n"

        return file
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun createJsonFile(packageName: String, dependencies: Set<KSFile>): OutputStream {
            val fileName = packageName.replace('.', '_') + "_gen"
            return codeGenerator.createNewFile(
                dependencies = Dependencies(false, *dependencies.toTypedArray()),
                packageName = packageName,
                fileName = fileName,
                extensionName = "json"
            )
        }

        // DesignDoc annotation
        val symbols =
            resolver
                .getSymbolsWithAnnotation("com.android.designcompose.annotation.DesignDoc")
                .filterIsInstance<
                    KSClassDeclaration
                >() // Making sure we take only class declarations.
        if (!symbols.iterator().hasNext()) return emptyList()

        // The Json output files are package-specific. Build a map of the package names to the
        // files that contribute symbols to the package.
        val perPackageSourceDependencies: HashMap<String, MutableSet<KSFile>> = HashMap()
        symbols.forEach {
            it.containingFile?.let(
                perPackageSourceDependencies.getOrPut(it.packageName.asString()) {
                    mutableSetOf<KSFile>()
                }::add
            )
        }

        // Use that map to create files for each package
        val jsonStreams: HashMap<String, OutputStream> = HashMap()
        perPackageSourceDependencies.forEach {
            jsonStreams[it.key] = createJsonFile(it.key, it.value.toSet())
        }

        // Visit each symbol and generate all files
        symbols.forEach { it.accept(DesignDocVisitor(jsonStreams), Unit) }

        // Finish up
        jsonStreams.values.forEach { it.close() }
        // val ret = symbols.filterNot { it.validate() }.toList()

        // Return emptyList instead of ret to avoid re-processing the output file infinitely. This
        // should be reverted once the output file is fully compilable, at which point `ret` should
        // be an empty list.
        return emptyList()
    }

    private enum class VisitPhase {
        Queries,
        NodeCustomizations,
        IgnoredImages,
        ComposableFunctions,
        KeyActionFunctions,
    }

    private enum class CustomizationType {
        Text,
        TextFunction,
        Image,
        ImageWithContext,
        Brush,
        BrushFunction,
        Modifier,
        TapCallback,
        ContentReplacement,
        ComponentReplacement,
        ListContent,
        Visibility,
        TextStyle,
        VariantProperty,
        Meter,
        MeterFunction,
        Unknown
    }

    inner class DesignDocVisitor(
        private val jsonStreams: HashMap<String, OutputStream>,
    ) : KSVisitorVoid() {
        private var docName: String = ""
        private var docId: String = ""
        private var currentFunc = ""
        private var textCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var textFunctionCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var imageCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var brushCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var brushFunctionCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var modifierCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var tapCallbackCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var contentCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var listCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var replacementCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var imageContextCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var visibleCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var textStyleCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var meterCustomizations: HashMap<String, Vector<Pair<String, String>>> = HashMap()
        private var meterFunctionCustomizations: HashMap<String, Vector<Pair<String, String>>> =
            HashMap()
        private var nodeNameBuilder: ArrayList<String> = ArrayList()
        private var variantProperties: HashMap<String, String> = HashMap()

        private var visitPhase: VisitPhase = VisitPhase.Queries
        private var queriesNameSet: HashSet<String> = HashSet()
        private var ignoredImages: HashMap<String, HashSet<String>> = HashMap()

        private var overrideInterface: String = ""

        private lateinit var out: OutputStream
        private var currentJsonStream: OutputStream? = null
        private var designDocJson: JsonObject = JsonObject()
        private var jsonComponents: JsonArray = JsonArray()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            if (classDeclaration.classKind != ClassKind.INTERFACE) {
                logger.error("Invalid class kind with @DesignDoc", classDeclaration)
                return
            }
            // Convenience
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()

            // Create a new file for each @DesignDoc annotation
            out = createNewFile(className, packageName, setOf(classDeclaration.containingFile!!))

            currentJsonStream = jsonStreams[packageName]

            // If the interface inherits from another interface, get the name of it
            classDeclaration.superTypes.forEach {
                val superType = it.resolve()
                // All classes that don't declare a superclass inherit from "Any", so ignore it
                if (superType.toString() != "Any") {
                    if (superType.isError)
                        logger.error("Invalid supertype for interface hg $classDeclaration")
                    overrideInterface = superType.declaration.qualifiedName?.asString() ?: ""
                    if (overrideInterface.endsWith("Gen"))
                        logger.error(
                            "Extending a generated interface $overrideInterface not supported"
                        )
                }
            }

            // Get the @DesignDoc annotation object
            val annotation: KSAnnotation =
                classDeclaration.annotations.first { it.shortName.asString() == "DesignDoc" }

            // Get the 'id' argument object from @DesignDoc.
            val idArg: KSValueArgument =
                annotation.arguments.first { arg -> arg.name?.asString() == "id" }
            docName = className + "Doc"
            docId = idArg.value as String

            // Declare a global document ID that can be changed by the Design Switcher
            val docIdVarName = className + "GenId"
            out.appendText("private var $docIdVarName: String = \"$docId\"\n\n")

            // Create an interface for each interface declaration
            val interfaceName = className + "Gen"
            // Inherit from the specified superclass interface if it exists
            val overrideInterfaceDecl =
                if (overrideInterface.isNotEmpty()) ": $overrideInterface " else ""
            out.appendText("interface $interfaceName $overrideInterfaceDecl{\n")

            // Create a list of all node names used in this interface
            visitPhase = VisitPhase.Queries
            out.appendText("    fun queries(): ArrayList<String> {\n")
            out.appendText("        return arrayListOf(\n")
            classDeclaration.getAllFunctions().forEach { it.accept(this, data) }
            out.appendText("        )\n")
            out.appendText("    }\n\n")

            // Create a list of all customization node names used in this interface
            visitPhase = VisitPhase.NodeCustomizations
            classDeclaration.getAllFunctions().forEach { it.accept(this, data) }

            // Iterate through all the functions and parameters and build up the ignoredImages
            // HashMap
            visitPhase = VisitPhase.IgnoredImages
            classDeclaration.getAllFunctions().forEach { it.accept(this, data) }

            // Output the list of all images to ignore using the ignoredImages HashMap
            out.appendText("    fun ignoredImages(): HashMap<String, Array<String>> {\n")
            out.appendText("        return hashMapOf(\n")
            for ((node, images) in ignoredImages) {
                out.appendText("            \"$node\" to arrayOf(\n")
                for (image in images) {
                    out.appendText("                \"$image\",\n")
                }
                out.appendText("            ),\n")
            }
            out.appendText("        )\n")
            out.appendText("    }\n\n")

            // Add a @Composable function that can be used to show the design switcher
            out.appendText("    @Composable\n")
            out.appendText("    fun DesignSwitcher(modifier: Modifier = Modifier) {\n")
            out.appendText(
                "        val (docId, setDocId) = remember { mutableStateOf(\"$docId\") }\n"
            )
            out.appendText("        DesignDoc(\"$docName\", docId, NodeQuery.NodeName(\"\"),\n")
            out.appendText("            modifier = modifier,\n")
            out.appendText("            setDocId = setDocId\n")
            out.appendText("        )\n")
            out.appendText("    }\n\n")

            // Add a function that lets you add the Design Switcher to a view group at the top right
            // corner
            out.appendText(
                "    fun addDesignSwitcherToViewGroup(activity: Activity, view: ViewGroup) {\n"
            )
            out.appendText(
                "        val composeView = ComposeView(activity.applicationContext).apply {\n"
            )
            out.appendText("            setContent { DesignSwitcher() }\n")
            out.appendText("        }\n")
            out.appendText("        var width: Int\n")
            out.appendText("        var height: Int\n")
            out.appendText("        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {\n")
            out.appendText(
                "            val displayMetrics = activity.windowManager.currentWindowMetrics\n"
            )
            out.appendText("            width = displayMetrics.bounds.width()\n")
            out.appendText("            height = displayMetrics.bounds.height()\n")
            out.appendText("        } else {\n")
            out.appendText("            val displayMetrics = DisplayMetrics()\n")
            out.appendText(
                "            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)\n"
            )
            out.appendText("            height = displayMetrics.heightPixels\n")
            out.appendText("            width = displayMetrics.widthPixels\n")
            out.appendText("        }\n")
            out.appendText("        val params = FrameLayout.LayoutParams(width, height)\n")
            out.appendText("        params.leftMargin = 0\n")
            out.appendText("        params.topMargin = 0\n")
            out.appendText("        view.addView(composeView, params)\n")
            out.appendText("    }\n\n")

            visitPhase = VisitPhase.ComposableFunctions
            classDeclaration.getAllFunctions().forEach { it.accept(this, data) }

            visitPhase = VisitPhase.KeyActionFunctions
            classDeclaration.getAllFunctions().forEach { it.accept(this, data) }

            // Add a function that composes a component with variants given a node name that
            // contains all the variant properties and values
            out.appendText("    @Composable\n")
            // Add an override declaration if this interface inherits from another
            val overrideDecl = if (overrideInterface.isNotEmpty()) "override " else ""
            out.appendText("    ${overrideDecl}fun CustomComponent(\n")
            // Default parameters are not allowed when overriding a function
            val defaultModifierDecl = if (overrideInterface.isEmpty()) " = Modifier" else ""
            out.appendText("        modifier: Modifier$defaultModifierDecl,\n")
            out.appendText("        nodeName: String,\n")
            out.appendText("        rootNodeQuery: NodeQuery,\n")
            out.appendText("        parentComponents: List<ParentComponentInfo>,\n")
            out.appendText("        tapCallback: TapCallback?,\n")
            out.appendText("    ) {\n")
            out.appendText("        val customizations = remember { CustomizationContext() }\n")
            out.appendText("        if (tapCallback != null)\n")
            out.appendText("            customizations.setTapCallback(nodeName, tapCallback)\n")
            out.appendText("        customizations.mergeFrom(LocalCustomizationContext.current)\n")
            out.appendText(
                "        val (docId, setDocId) = remember { mutableStateOf(\"$docId\") }\n"
            )
            out.appendText("        val queries = queries()\n")
            out.appendText("        queries.add(nodeName)\n")
            out.appendText(
                "        CompositionLocalProvider(LocalCustomizationContext provides customizations) {\n"
            )
            out.appendText("            DesignDoc(\"$docName\", docId, rootNodeQuery,\n")
            out.appendText("                customizations = customizations,\n")
            out.appendText("                modifier = modifier,\n")
            out.appendText(
                "                serverParams = DocumentServerParams(queries, ignoredImages()),\n"
            )
            out.appendText("                setDocId = setDocId,\n")
            out.appendText(
                "                designSwitcherPolicy = DesignSwitcherPolicy.SHOW_IF_ROOT,\n"
            )
            out.appendText("                parentComponents = parentComponents\n")
            out.appendText("            )\n")
            out.appendText("        }\n")
            out.appendText("    }\n\n")

            out.appendText("}\n\n")

            val objectName = className + "Doc"
            out.appendText("object $objectName: $interfaceName {}\n\n")

            // Write the design doc JSON for our plugin
            designDocJson.addProperty("name", className)
            designDocJson.add("components", jsonComponents)
            val versionArg = annotation.arguments.find { arg -> arg.name?.asString() == "version" }
            if (versionArg != null) {
                val versionString = versionArg.value as String
                designDocJson.addProperty("version", versionString)
            }

            val gson = GsonBuilder().setPrettyPrinting().create()
            currentJsonStream?.appendText(gson.toJson(designDocJson))

            // Close out the main file
            out.close()
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            if (function.functionKind != FunctionKind.MEMBER) {
                logger.error(
                    "Invalid function kind ${function.functionKind} with @DesignDoc",
                    function
                )
                return
            }

            if (visitPhase == VisitPhase.KeyActionFunctions) {
                // For the KeyActionFunctions visit phase, check for a @DesignKeyAction annotation
                // and generate the function to inject a key event
                val keyAnnotation: KSAnnotation? =
                    function.annotations.find { it.shortName.asString() == "DesignKeyAction" }
                if (keyAnnotation != null) visitDesignKeyAction(function, keyAnnotation)
            } else {
                // Get the @DesignComponent annotation object, or return if none
                val annotation: KSAnnotation? =
                    function.annotations.find { it.shortName.asString() == "DesignComponent" }

                currentFunc = function.toString()
                if (annotation != null) visitDesignComponent(function, data, annotation)
            }
        }

        private fun visitDesignComponent(
            function: KSFunctionDeclaration,
            data: Unit,
            annotation: KSAnnotation
        ) {
            // Get the 'node' argument
            val nodeArg: KSValueArgument =
                annotation.arguments.first { arg -> arg.name?.asString() == "node" }
            val nodeName = nodeArg.value as String

            // Get the 'isRoot' argument
            val isRootArg = annotation.arguments.find { arg -> arg.name?.asString() == "isRoot" }
            val isRoot = if (isRootArg != null) isRootArg.value as Boolean else false

            // Get the 'override' argument, which should be set if this interface inherits from
            // another interface and this function overrides one of the base functions
            val overrideArg: KSValueArgument? =
                annotation.arguments.find { it.name?.asString() == "override" }
            var override = false
            if (overrideArg != null) override = overrideArg.value as Boolean

            when (visitPhase) {
                VisitPhase.Queries -> visitFunctionQueries(function, nodeName)
                VisitPhase.NodeCustomizations ->
                    visitFunctionNodeCustomizations(function, nodeName, isRoot)
                VisitPhase.IgnoredImages -> visitFunctionIgnoredImagesBuild(function, nodeName)
                VisitPhase.ComposableFunctions ->
                    visitFunctionComposables(function, data, nodeName, override)
                VisitPhase.KeyActionFunctions -> {}
            }
        }

        private fun visitDesignKeyAction(
            function: KSFunctionDeclaration,
            annotation: KSAnnotation
        ) {
            // Get the 'key' argument
            val keyArg: KSValueArgument =
                annotation.arguments.first { arg -> arg.name?.asString() == "key" }
            val key = keyArg.value as Char

            // Get the 'metaKeys' argument
            val metaKeysArg: KSValueArgument =
                annotation.arguments.first { arg -> arg.name?.asString() == "metaKeys" }

            // Generate a comma separated list of metakeys
            var metaKeysStr = ""
            if (metaKeysArg.value != null) {
                val metaKeys = metaKeysArg.value as ArrayList<DesignMetaKey>
                metaKeysStr = metaKeys.joinToString(",")
            }

            out.appendText("    fun $function() {\n")
            out.appendText("        DesignInjectKey('$key', listOf($metaKeysStr))\n ")
            out.appendText("    }\n\n")
        }

        private fun visitFunctionQueries(function: KSFunctionDeclaration, nodeName: String) {
            if (!queriesNameSet.contains(nodeName)) {
                out.appendText("            \"$nodeName\",\n")
                queriesNameSet.add(nodeName)
            }

            // If there are any @DesignVariant annotations, add the property names to the list of
            // queries
            function.parameters.forEach { param ->
                val annotation: KSAnnotation? =
                    param.annotations.find { it.shortName.asString() == "DesignVariant" }
                if (annotation != null) {
                    val propertyArg: KSValueArgument =
                        annotation.arguments.first { arg -> arg.name?.asString() == "property" }
                    val propertyName = propertyArg.value as String
                    if (!queriesNameSet.contains(propertyName)) {
                        out.appendText("            \"$propertyName\",\n")
                        queriesNameSet.add(propertyName)
                    }
                }
            }
        }

        @OptIn(KspExperimental::class)
        private fun visitFunctionNodeCustomizations(
            function: KSFunctionDeclaration,
            node: String,
            isRoot: Boolean
        ) {
            // Add the node and function name to the json components list
            val jsonComponent = JsonObject()
            jsonComponent.addProperty("node", node)
            jsonComponent.addProperty("name", function.toString())
            jsonComponent.addProperty("isRoot", isRoot)
            jsonComponents.add(jsonComponent)
            val jsonCustomizations = JsonArray()

            function.parameters.forEach { param ->
                val annotation: KSAnnotation =
                    param.annotations.find {
                        it.shortName.asString() == "Design" ||
                            it.shortName.asString() == "DesignVariant"
                    } ?: return
                val nodeArg: KSValueArgument =
                    annotation.arguments.first { arg ->
                        arg.name?.asString() == "node" || arg.name?.asString() == "property"
                    }
                val nodeName = nodeArg.value as String

                // Add a customization for this arg to the json list
                val jsonHash = JsonObject()
                val paramType = getParamCustomizationType(param)
                jsonHash.addProperty("name", param.name?.asString())
                jsonHash.addProperty("node", nodeName)
                jsonHash.addProperty("kind", paramType.name)

                // If there is a @DesignContentTypes annotation, use it to build a content list
                // for this node. This represents all node names that could be placed into this
                // node as a child.
                val contentTypesAnnotation =
                    param.annotations.find { it.shortName.asString() == "DesignContentTypes" }
                if (contentTypesAnnotation != null) {
                    @Suppress("UNCHECKED_CAST")
                    val nodes =
                        contentTypesAnnotation.arguments
                            .first { arg -> arg.name?.asString() == "nodes" }
                            .value as? ArrayList<String>
                    val jsonContentArray = JsonArray()
                    nodes?.forEach { jsonContentArray.add(it.trim()) }
                    jsonHash.add("content", jsonContentArray)
                }

                // To get data for the @DesignPreviewContent annotation, we need to use
                // getAnnotationsByType() in order to parse out the custom class PreviewNode.
                val designPreviewContentAnnotations =
                    param.getAnnotationsByType(DesignPreviewContent::class)

                val previewContentArray = JsonArray()
                designPreviewContentAnnotations.forEach { content ->
                    val previewPageHash = JsonObject()
                    val jsonContentArray = JsonArray()
                    content.nodes.forEach {
                        for (i in 1..it.count) {
                            jsonContentArray.add(it.node.trim())
                        }
                    }
                    if (!jsonContentArray.isEmpty) {
                        previewPageHash.addProperty("name", content.name)
                        previewPageHash.add("content", jsonContentArray)
                        previewContentArray.add(previewPageHash)
                    }
                }
                if (!previewContentArray.isEmpty)
                    jsonHash.add("previewContent", previewContentArray)

                jsonCustomizations.add(jsonHash)

                val classDecl = param.type.resolve().declaration.closestClassDeclaration()
                if (classDecl != null && classDecl.classKind == ClassKind.ENUM_CLASS) {
                    val jsonVariantsArray = JsonArray()
                    classDecl.declarations.forEach {
                        val enumValue = it.simpleName.asString()
                        if (
                            enumValue != "valueOf" && enumValue != "values" && enumValue != "<init>"
                        ) {
                            jsonVariantsArray.add(it.simpleName.asString())
                        }
                    }
                    jsonHash.add("values", jsonVariantsArray)
                }
            }

            jsonComponent.add("customizations", jsonCustomizations)
        }

        private fun visitFunctionIgnoredImagesBuild(
            function: KSFunctionDeclaration,
            nodeName: String
        ) {
            val nodeImageSet = ignoredImages[nodeName] ?: HashSet<String>()
            function.parameters.forEach { param ->
                val ignore =
                    when (getParamCustomizationType(param)) {
                        CustomizationType.Image -> true
                        CustomizationType.Brush -> true
                        CustomizationType.BrushFunction -> true
                        CustomizationType.ContentReplacement -> true
                        CustomizationType.ComponentReplacement -> true
                        CustomizationType.ListContent -> true
                        CustomizationType.ImageWithContext -> true
                        else -> false
                    }

                // Get the 'node' argument
                val annotation: KSAnnotation =
                    param.annotations.find { it.shortName.asString() == "Design" } ?: return
                val nodeArg: KSValueArgument =
                    annotation.arguments.first { arg -> arg.name?.asString() == "node" }
                val paramNodeName = nodeArg.value as String

                if (ignore) nodeImageSet.add(paramNodeName)
            }
            ignoredImages[nodeName] = nodeImageSet
        }

        private fun getParamTypeString(param: KSValueParameter): String {
            // Add any annotations specified for this type
            val typeAnnotations = param.type.annotations
            var typeName = ""
            typeAnnotations.forEach { typeName += "$it " }
            val ksType = param.type.resolve()
            val qualifiedName = ksType.declaration.qualifiedName
            val qualifier = qualifiedName?.getQualifier()
            // For kotlin and android types, use just the typename without the qualifier. Otherwise,
            // use the qualifier, since the macro specified an explicit qualifier
            typeName +=
                if (qualifier?.startsWith("kotlin")?.or(qualifier.startsWith("android")) == true)
                    param.type.toString()
                else qualifiedName?.asString() ?: param.type.toString()

            // Add template parameters if there are any
            if (!ksType.isFunctionType && ksType.arguments.isNotEmpty()) {
                typeName += "<${ksType.arguments.joinToString(",") { arg -> arg.type.toString() }}>"
            }

            // Add nullability operator to types in typeName that are nullable
            ksType.arguments.forEach {
                if (it.type?.resolve()?.nullability == Nullability.NULLABLE)
                    typeName = typeName.replace(it.type.toString(), "${it.type}?")
            }

            // Add Nullability operator if the type is nullable
            if (param.type.resolve().nullability == Nullability.NULLABLE) typeName += "?"

            return typeName
        }

        private fun getParamCustomizationType(param: KSValueParameter): CustomizationType {
            val variantAnnotation: KSAnnotation? =
                param.annotations.find { it.shortName.asString() == "DesignVariant" }
            if (variantAnnotation != null) return CustomizationType.VariantProperty

            return when (getParamTypeString(param)) {
                "String" -> CustomizationType.Text
                "@Composable () -> String" -> CustomizationType.TextFunction
                "Brush" -> CustomizationType.Brush
                "() -> Brush" -> CustomizationType.BrushFunction
                "Bitmap?" -> CustomizationType.Image
                "Modifier" -> CustomizationType.Modifier
                "com.android.designcompose.TapCallback" -> CustomizationType.TapCallback
                "com.android.designcompose.ReplacementContent" ->
                    CustomizationType.ContentReplacement
                "@Composable (ComponentReplacementContext) -> Unit" ->
                    CustomizationType.ComponentReplacement
                "com.android.designcompose.ListContent" -> CustomizationType.ListContent
                "@Composable (ImageReplacementContext) -> Bitmap?" ->
                    CustomizationType.ImageWithContext
                "Boolean" -> CustomizationType.Visibility
                "TextStyle" -> CustomizationType.TextStyle
                "com.android.designcompose.Meter" -> CustomizationType.Meter
                "com.android.designcompose.MeterFunction" -> CustomizationType.MeterFunction
                else -> CustomizationType.Unknown
            }
        }

        private fun visitFunctionComposables(
            function: KSFunctionDeclaration,
            data: Unit,
            nodeName: String,
            override: Boolean
        ) {
            // Generate the function name and args
            out.appendText("    @Composable\n")
            val overrideKeyword = if (override) "override " else ""
            out.appendText("    ${overrideKeyword}fun $function(\n")

            // Collect arguments into a list so we can reuse them
            val args: ArrayList<Pair<String, String>> = ArrayList()

            // Add a modifier for the root item. Default params not allowed with override
            val defaultModifier = if (override) "" else " = Modifier"
            args.add(Pair("modifier", "Modifier$defaultModifier"))

            // Add an option to register a function to handle open link callbacks
            val defaultOpenLink = if (override) "" else " = null"
            args.add(Pair("openLinkCallback", "OpenLinkCallback?$defaultOpenLink"))

            // Add optional callbacks to be called on certain document events
            val defaultCallbacks = if (override) "" else " = null"
            args.add(Pair("designComposeCallbacks", "DesignComposeCallbacks?$defaultCallbacks"))
            val defaultDesignDocSettings = if (override) "" else " = null"
            args.add(Pair("designDocSettings", "DesignDocSettings?$defaultDesignDocSettings"))

            // Add optional key that can be used to uniquely identify this particular instance
            val keyDefault = if (override) "" else " = null"
            args.add(Pair("key", "String?$keyDefault"))

            // Add an optional replacement index that should be populated if the composable is
            // replacing children of a node through a content replacement customization

            // Get the @DesignComponent annotation object, or return if none
            val annotation: KSAnnotation =
                function.annotations.find { it.shortName.asString() == "DesignComponent" } ?: return

            // Get the 'designSwitcher' argument
            val switcherArg: KSValueArgument? =
                annotation.arguments.find { it.name?.asString() == "hideDesignSwitcher" }
            var hideDesignSwitcher = false
            if (switcherArg != null) hideDesignSwitcher = switcherArg.value as Boolean

            // placeholder is a special name that gets mapped to a placeholder composable.
            var placeholderComposable: String? = null

            // variantFuncParameters accumulates a list of all the DesignVariant parameters and is
            // used
            // when generating the *Node function after this one.
            var variantFuncParameters = ""
            nodeNameBuilder.clear()
            variantProperties.clear()
            function.parameters.forEach { param ->
                // Each parameter may have an annotation, so visit each one
                param.accept(this, data)
                val name = param.name!!.asString()

                val customizationType = getParamCustomizationType(param)
                val typeName = getParamTypeString(param)
                args.add(Pair(name, typeName))
                if (customizationType == CustomizationType.VariantProperty)
                    variantFuncParameters += "        $name: $typeName,\n"

                if (
                    name == "placeholder" &&
                        customizationType == CustomizationType.ContentReplacement
                )
                    placeholderComposable = name
            }

            // Output all arguments
            args.forEach { out.appendText("        ${it.first}: ${it.second},\n") }

            // If there are any DesignVariant annotations on parameters, nodeNameBuilder would have
            // been populated. If the node name matches a property in variantProperties, we assume
            // that it is a component set, so we construct a node name that represents one of its
            // child variants and make rootNodeQuery a NodeVariant.
            val isComponentSet = variantProperties.containsKey(nodeName)
            val nodeNameVar =
                if (isComponentSet) {
                    nodeNameBuilder.joinToString(" + \",\"\n") + "\n"
                } else {
                    ""
                }

            // Generate the function body by filling out customizations and then returning
            // the @Composable DesignDoc function
            out.appendText("    ) {\n")
            // Embed the Doc's class to allow matching on in tests
            out.appendText("        val className = javaClass.name\n")
            out.appendText("        val customizations = remember { CustomizationContext() }\n")
            out.appendText("        customizations.setKey(key)\n")
            out.appendText("        customizations.mergeFrom(LocalCustomizationContext.current)\n")

            // Set the node name. If this is a component with variants, the node name has been built
            // in the nodeNameVar variable. Otherwise, it's just the node name passed in.
            if (isComponentSet) {
                out.appendText("        var nodeName = \"\"\n")
                out.appendText(nodeNameVar)
            } else {
                out.appendText("        var nodeName = \"$nodeName\"\n")
            }

            // Create a NodeQuery for the initial root node
            if (isComponentSet)
                out.appendText(
                    "        val rootNodeQuery = NodeQuery.NodeVariant(nodeName, \"$nodeName\")\n"
                )
            else out.appendText("        val rootNodeQuery = NodeQuery.NodeName(nodeName)\n")

            // Register the open link callback if one exists
            out.appendText("        if (openLinkCallback != null)\n")
            out.appendText(
                "            customizations.setOpenLinkCallback(nodeName, openLinkCallback)\n"
            )

            val textCustom =
                textCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in textCustom) {
                out.appendText("        customizations.setText(\"$node\", $value)\n")
            }

            val textFuncCustom =
                textFunctionCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in textFuncCustom) {
                out.appendText("        customizations.setTextFunction(\"$node\", $value)\n")
            }

            val imageCustom =
                imageCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in imageCustom) {
                out.appendText("        customizations.setImage(\"$node\", $value)\n")
            }

            val brushCustom =
                brushCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in brushCustom) {
                out.appendText("        customizations.setBrush(\"$node\", $value)\n")
            }

            val brushFunctionCustom =
                brushFunctionCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in brushFunctionCustom) {
                out.appendText("        customizations.setBrushFunction(\"$node\", $value)\n")
            }

            val modifierCustom =
                modifierCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in modifierCustom) {
                out.appendText("        customizations.setModifier(\"$node\", $value)\n")
            }

            val tapCallbackCustom =
                tapCallbackCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in tapCallbackCustom) {
                out.appendText("        customizations.setTapCallback(\"$node\", $value)\n")
            }

            val contentCustom =
                contentCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in contentCustom) {
                out.appendText("        customizations.setContent(\"$node\", $value)\n")
            }

            val listCustom =
                listCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in listCustom) {
                out.appendText("        customizations.setListContent(\"$node\", $value)\n")
            }

            val replacementCustom =
                replacementCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in replacementCustom) {
                out.appendText("        customizations.setComponent(\"$node\", $value)\n")
            }

            val imageContextCustom =
                imageContextCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in imageContextCustom) {
                out.appendText("        customizations.setImageWithContext(\"$node\", $value)\n")
            }

            val visibleCustom =
                visibleCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in visibleCustom) {
                out.appendText("        customizations.setVisible(\"$node\", $value)\n")
            }

            val textStyleCustom =
                textStyleCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in textStyleCustom) {
                out.appendText("        customizations.setTextStyle(\"$node\", $value)\n")
            }

            val meterCustom =
                meterCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in meterCustom) {
                out.appendText("        customizations.setMeterValue(\"$node\", $value)\n")
            }

            val meterFunctionCustom =
                meterFunctionCustomizations[function.toString()] ?: Vector<Pair<String, String>>()
            for ((node, value) in meterFunctionCustom) {
                out.appendText("        customizations.setMeterFunction(\"$node\", $value)\n")
            }

            out.appendText("\n")

            // Generate code to set variant properties if there are any @DesignVariant parameters
            if (variantProperties.isNotEmpty()) {
                out.appendText("        val variantProperties = HashMap<String, String>()\n")
                variantProperties.forEach {
                    out.appendText("        variantProperties[\"${it.key}\"] = ${it.value}\n")
                }
                out.appendText("        customizations.setVariantProperties(variantProperties)\n")
            }
            out.appendText(
                "        customizations.setCustomComposable { mod, name, query, parentComponents, tapCallback ->\n"
            )
            out.appendText(
                "            CustomComponent(mod, name, query, parentComponents, tapCallback) }\n\n"
            )

            // Create a mutable state so that the Design Switcher can dynamically change the
            // document ID
            out.appendText(
                "        val (docId, setDocId) = remember { mutableStateOf(\"$docId\") }\n"
            )

            // If there are variants, add the variant name to the list of queries
            out.appendText("        val queries = queries()\n")
            if (isComponentSet) out.appendText("        queries.add(nodeName)\n")

            out.appendText(
                "        CompositionLocalProvider(LocalCustomizationContext provides customizations) {\n"
            )
            out.appendText("            DesignDoc(\"$docName\", docId, rootNodeQuery,\n")
            if (placeholderComposable != null) {
                out.appendText("                placeholder = $placeholderComposable,")
            }
            out.appendText("                customizations = customizations,\n")
            out.appendText(
                "                modifier = modifier.semantics { sDocClass = className},\n"
            )
            out.appendText("                squoosh = designDocSettings?.enableSquoosh?: false,\n")
            out.appendText(
                "                serverParams = DocumentServerParams(queries, ignoredImages()),\n"
            )
            out.appendText("                setDocId = setDocId,\n")
            val switchPolicy =
                if (hideDesignSwitcher) "DesignSwitcherPolicy.HIDE"
                else "DesignSwitcherPolicy.SHOW_IF_ROOT"
            out.appendText("                designSwitcherPolicy = $switchPolicy,\n")
            out.appendText(
                "                designComposeCallbacks = designDocSettings?.designComposeCallbacks,\n"
            )
            out.appendText("            )\n")
            out.appendText("        }\n")
            out.appendText("    }\n\n")

            // New function for DesignNodeData
            out.appendText("    ${overrideKeyword}fun ${function}DesignNodeData(\n")
            out.appendText(variantFuncParameters)
            out.appendText("    ): DesignNodeData {\n")
            out.appendText("        val variantProperties = HashMap<String, String>()\n")
            if (variantProperties.isNotEmpty()) {
                variantProperties.forEach {
                    out.appendText("        variantProperties[\"${it.key}\"] = ${it.value}\n")
                }
            }
            out.appendText("        return DesignNodeData(\"$nodeName\", variantProperties)\n")
            out.appendText("    }\n\n")
        }

        // Visit a function parameter. For each parameter, check all parameter annotations
        override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
            val variantAnnotation: KSAnnotation? =
                valueParameter.annotations.find { it.shortName.asString() == "DesignVariant" }
            if (variantAnnotation != null) {
                val propertyArg: KSValueArgument =
                    variantAnnotation.arguments.first { arg -> arg.name?.asString() == "property" }
                val propertyName = propertyArg.value as String
                val param = valueParameter.name!!.asString()

                variantProperties[propertyName] = "$param.name"
                nodeNameBuilder.add("        nodeName += \"$propertyName=\" + $param.name")
            }

            val annotation: KSAnnotation =
                valueParameter.annotations.find { it.shortName.asString() == "Design" } ?: return

            when (getParamCustomizationType(valueParameter)) {
                CustomizationType.Text ->
                    addCustomization(valueParameter, annotation, textCustomizations)
                CustomizationType.TextFunction ->
                    addCustomization(valueParameter, annotation, textFunctionCustomizations)
                CustomizationType.Image ->
                    addCustomization(valueParameter, annotation, imageCustomizations)
                CustomizationType.Brush ->
                    addCustomization(valueParameter, annotation, brushCustomizations)
                CustomizationType.BrushFunction ->
                    addCustomization(valueParameter, annotation, brushFunctionCustomizations)
                CustomizationType.Modifier ->
                    addCustomization(valueParameter, annotation, modifierCustomizations)
                CustomizationType.TapCallback ->
                    addCustomization(valueParameter, annotation, tapCallbackCustomizations)
                CustomizationType.ContentReplacement ->
                    addCustomization(valueParameter, annotation, contentCustomizations)
                CustomizationType.ComponentReplacement ->
                    addCustomization(valueParameter, annotation, replacementCustomizations)
                CustomizationType.ListContent ->
                    addCustomization(valueParameter, annotation, listCustomizations)
                CustomizationType.ImageWithContext ->
                    addCustomization(valueParameter, annotation, imageContextCustomizations)
                CustomizationType.Visibility ->
                    addCustomization(valueParameter, annotation, visibleCustomizations)
                CustomizationType.TextStyle ->
                    addCustomization(valueParameter, annotation, textStyleCustomizations)
                CustomizationType.Meter ->
                    addCustomization(valueParameter, annotation, meterCustomizations)
                CustomizationType.MeterFunction ->
                    addCustomization(valueParameter, annotation, meterFunctionCustomizations)
                else ->
                    logger.error(
                        "Invalid @Design parameter type ${getParamTypeString(valueParameter)}"
                    )
            }
        }

        private fun addCustomization(
            valueParameter: KSValueParameter,
            annotation: KSAnnotation,
            customizations: HashMap<String, Vector<Pair<String, String>>>
        ) {
            val nodeArg: KSValueArgument =
                annotation.arguments.first { arg -> arg.name?.asString() == "node" }
            val node = nodeArg.value as String
            val name = valueParameter.name!!.asString()

            val vec = customizations[currentFunc] ?: Vector<Pair<String, String>>()
            vec.add(Pair(node, name))
            customizations[currentFunc] = vec
        }

        override fun visitNode(node: KSNode, data: Unit) {}

        override fun visitAnnotated(annotated: KSAnnotated, data: Unit) {}

        override fun visitAnnotation(annotation: KSAnnotation, data: Unit) {}

        override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: Unit) {}

        override fun visitDeclaration(declaration: KSDeclaration, data: Unit) {}

        override fun visitDeclarationContainer(
            declarationContainer: KSDeclarationContainer,
            data: Unit
        ) {}

        override fun visitDynamicReference(reference: KSDynamicReference, data: Unit) {}

        override fun visitFile(file: KSFile, data: Unit) {}

        override fun visitCallableReference(reference: KSCallableReference, data: Unit) {}

        override fun visitParenthesizedReference(reference: KSParenthesizedReference, data: Unit) {}

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {}

        override fun visitPropertyAccessor(accessor: KSPropertyAccessor, data: Unit) {}

        override fun visitPropertyGetter(getter: KSPropertyGetter, data: Unit) {}

        override fun visitPropertySetter(setter: KSPropertySetter, data: Unit) {}

        override fun visitClassifierReference(reference: KSClassifierReference, data: Unit) {}

        override fun visitReferenceElement(element: KSReferenceElement, data: Unit) {}

        override fun visitTypeAlias(typeAlias: KSTypeAlias, data: Unit) {}

        override fun visitTypeArgument(typeArgument: KSTypeArgument, data: Unit) {}

        override fun visitTypeParameter(typeParameter: KSTypeParameter, data: Unit) {}

        override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {}

        override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {}
    }
}

class BuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return BuilderProcessor(environment.codeGenerator, environment.logger)
    }
}

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
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.OutputStream
import java.util.Vector
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class BuilderProcessor(private val codeGenerator: CodeGenerator, val logger: KSPLogger) :
    SymbolProcessor {

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

        // Process DesignModules first
        val moduleNodeNameTable = processDesignModulesClasses(resolver, codeGenerator, logger)

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
        symbols.forEach { it.accept(DesignDocVisitor(moduleNodeNameTable, jsonStreams), Unit) }

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

    inner class DesignDocVisitor(
        private val moduleNodeNameTable: ModuleNodeNameTable,
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
        private var moduleCustomizations: HashMap<String, Vector<String>> = HashMap()
        private var nodeNameBuilder: ArrayList<String> = ArrayList()
        private var variantProperties: HashMap<String, String> = HashMap()

        private var visitPhase: VisitPhase = VisitPhase.Queries
        private var queriesNameSet: HashSet<String> = HashSet()
        private var ignoredImages: HashMap<String, HashSet<String>> = HashMap()

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
            out =
                createNewFile(
                    codeGenerator,
                    className,
                    packageName,
                    setOf(classDeclaration.containingFile!!),
                )

            currentJsonStream = jsonStreams[packageName]

            // We don't support inheritance from another interface. Check for this and emit an
            // error if we find a superclass.
            classDeclaration.superTypes.forEach {
                val superType = it.resolve()
                // All classes that don't declare a superclass inherit from "Any", so ignore it
                if (superType.toString() != "Any") {
                    val overrideInterface = superType.declaration.qualifiedName?.asString() ?: ""
                    logger.error("Extending a generated interface $overrideInterface not supported")
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
            out.appendText("class $interfaceName {\n")

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
            out.appendText("    final fun DesignSwitcher(modifier: Modifier = Modifier) {\n")
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
            out.appendText("    final fun CustomComponent(\n")
            out.appendText("        modifier: Modifier,\n")
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
            out.appendText("val $objectName = $interfaceName()\n\n")

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

            when (visitPhase) {
                VisitPhase.Queries -> visitFunctionQueries(function, nodeName)
                VisitPhase.NodeCustomizations ->
                    visitFunctionNodeCustomizations(function, nodeName, isRoot)
                VisitPhase.IgnoredImages -> visitFunctionIgnoredImagesBuild(function, nodeName)
                VisitPhase.ComposableFunctions -> visitFunctionComposables(function, data, nodeName)
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

            function.parameters.forEach { param ->
                // Add variants to the set of node queries
                val variantProperty = param.getVariantProperty()
                variantProperty?.let {
                    if (!queriesNameSet.contains(it)) {
                        out.appendText("            \"$it\",\n")
                        queriesNameSet.add(it)
                    }
                }

                // If a parameter is a module, add node queries from that module
                if (param.customizationType() == CustomizationType.Module) {
                    val moduleClassName = param.type.typeString()
                    val queries = moduleNodeNameTable.getNodeQueries(moduleClassName)
                    queries.forEach {
                        if (!queriesNameSet.contains(it)) {
                            out.appendText("            \"$it\",\n")
                            queriesNameSet.add(it)
                        }
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
                val paramType = param.customizationType()
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
            val nodeImageSet = ignoredImages[nodeName] ?: HashSet()
            function.parameters.forEach { param ->
                // Add ignored images from modules
                if (param.customizationType() == CustomizationType.Module) {
                    val moduleClassName = param.type.typeString()
                    val ignoredFromModule = moduleNodeNameTable.getIgnoredImages(moduleClassName)
                    ignoredFromModule?.let { nodeImageSet.addAll(it) }
                }

                // Add ignored images from other @Design parameters
                if (param.customizationType().shouldIgnoreImage())
                    param.getAnnotatedNodeName()?.let { nodeImageSet.add(it) }
            }
            ignoredImages[nodeName] = nodeImageSet
        }

        private fun visitFunctionComposables(
            function: KSFunctionDeclaration,
            data: Unit,
            nodeName: String,
        ) {
            // Generate the function name and args
            out.appendText("    @Composable\n")
            out.appendText("    final fun $function(\n")

            // Collect arguments into a list so we can reuse them
            val args: ArrayList<Pair<String, String>> = ArrayList()

            // Add a modifier for the root item.
            args.add(Pair("modifier", "Modifier = Modifier"))

            // Add an option to register a function to handle open link callbacks
            args.add(Pair("openLinkCallback", "OpenLinkCallback? = null"))

            // Add optional callbacks to be called on certain document events
            args.add(Pair("designComposeCallbacks", "DesignComposeCallbacks? = null"))

            // Add optional key that can be used to uniquely identify this particular instance
            args.add(Pair("key", "String? = null"))

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

                val customizationType = param.customizationType()
                val typeName = param.type.typeString()
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

            val moduleCustom = moduleCustomizations[function.toString()] ?: Vector()
            for (value in moduleCustom) {
                out.appendText("        customizations.mergeFrom($value.customizations())\n")
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
            out.appendText(
                "                serverParams = DocumentServerParams(queries, ignoredImages()),\n"
            )
            out.appendText("                setDocId = setDocId,\n")
            val switchPolicy =
                if (hideDesignSwitcher) "DesignSwitcherPolicy.HIDE"
                else "DesignSwitcherPolicy.SHOW_IF_ROOT"
            out.appendText("                designSwitcherPolicy = $switchPolicy,\n")
            out.appendText("                designComposeCallbacks = designComposeCallbacks,\n")
            out.appendText("            )\n")
            out.appendText("        }\n")
            out.appendText("    }\n\n")

            // New function for DesignNodeData
            out.appendText("    fun ${function}DesignNodeData(\n")
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

            val annotation =
                valueParameter.annotations.find {
                    val annotationName = it.shortName.asString()
                    annotationName == "Design" || annotationName == "DesignModule"
                } ?: return

            when (valueParameter.customizationType()) {
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
                CustomizationType.Module -> {
                    valueParameter.name?.let {
                        val name = it.asString()
                        val vec = moduleCustomizations[currentFunc] ?: Vector()
                        vec.add(name)
                        moduleCustomizations[currentFunc] = vec
                    }
                }
                else ->
                    logger.error(
                        "Invalid @Design parameter type ${valueParameter.type.typeString()}"
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

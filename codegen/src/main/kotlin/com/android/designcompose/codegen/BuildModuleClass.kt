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
import java.io.OutputStream

internal fun processDesignModulesClasses(
    resolver: Resolver,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) {
    // Hash of class name to list of queries used by that class
    val queries: HashMap<String, HashSet<String>> = HashMap()
    val ignoredImages: HashMap<String, HashSet<String>> = HashMap()

    val moduleClasses =
        resolver
            .getSymbolsWithAnnotation("com.android.designcompose.annotation.DesignModuleClass")
            .filterIsInstance<KSClassDeclaration>() // Making sure we take only class declarations.
    moduleClasses.forEach {
        it.accept(
            DesignModuleVisitor(
                codeGenerator,
                logger,
                queries,
                ignoredImages,
            ),
            Unit
        )
    }
}

class DesignModuleVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val queries: HashMap<String, HashSet<String>>,
    private val ignoredImages: HashMap<String, HashSet<String>>,
) : KSVisitorVoid() {
    private lateinit var out: OutputStream
    private lateinit var className: String

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()

        logger.warn("### DesignModule $className")
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.error(
                "### @DesignModuleClass must be used with a class. ${classDeclaration.classKind.type} not supported"
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
                logger
            )

        out += "fun $className.customizations2(): CustomizationContext {\n"
        out += "    val customizations = CustomizationContext()\n"

        val properties = classDeclaration.declarations
        properties.forEach { property -> property.accept(this, data) }

        out += "    return customizations\n"
        out += "}\n"

        logger.warn("### Queries: ${queries[className]}")
        logger.warn("### IgnoredImages: ${ignoredImages[className]}")

        out.close()
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
        val nodeName = getAnnotatedNode(property)
        val propertyName = property.simpleName.asString()
        /*
        val annotation: KSAnnotation =
            property.annotations.find { it.shortName.asString() == "Design2" } ?: return
        val node = annotation.arguments.find { it.name?.asString() == "node" } ?: return
        val nodeName = node.value.toString()
        logger.warn("  ### annotation ${annotation.shortName.asString()} node $nodeName")
         */
        // Skip non-annotated properties
        if (property.annotations.asIterable().toList().isEmpty())
            return

        if (nodeName != null)
            logger.warn("  ### field ${property.simpleName.asString()} nodeName $nodeName")
        nodeName?.let {
            addNodeToQueries(nodeName)
            if (property.customizationType().shouldIgnoreImage())
                addIgnoredImage(nodeName)
        }

        property.annotations.forEach {
            logger.warn("  ### annotation ${it.shortName.asString()}")
        }
        val customizationType = property.customizationType()
        when (customizationType) {
            CustomizationType.Text ->
                out += "    customizations.setText(\"$nodeName\", $propertyName)\n"
            CustomizationType.TextFunction ->
                out += "    customizations.setTextFunction(\"$nodeName\", $propertyName)\n"
            CustomizationType.Image ->
                out += "    customizations.setImage(\"$nodeName\", $propertyName)\n"
            /*
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

             */
            CustomizationType.Module ->
                out += "    customizations.mergeFrom($propertyName.customizations2())\n"
            else ->
                logger.error(
                    "Invalid @Design parameter type for property \"$propertyName\""
                )
        }
    }

    private fun addNodeToQueries(nodeName: String) {
        if (!queries.containsKey(className))
            queries[className] = HashSet()
        queries[className]?.add(nodeName)
    }

    private fun addIgnoredImage(nodeName: String) {
        if (!ignoredImages.containsKey(className))
            ignoredImages[className] = HashSet()
        ignoredImages[className]?.add(nodeName)
    }
}

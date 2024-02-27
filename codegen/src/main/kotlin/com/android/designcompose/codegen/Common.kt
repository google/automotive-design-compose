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
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import java.io.OutputStream

internal enum class CustomizationType {
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
    Module,
    Unknown
}

operator fun OutputStream.plusAssign(str: String) {
    this.write(str.toByteArray())
}

fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

internal fun createNewFile(
    codeGenerator: CodeGenerator,
    className: String,
    packageName: String,
    dependencies: Set<KSFile>,
    logger: KSPLogger,
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

internal fun KSTypeReference.typeString(): String {
    // Add any annotations specified for this type
    var typeName = ""
    annotations.forEach { typeName += "$it " }
    val ksType = resolve()
    val qualifiedName = ksType.declaration.qualifiedName
    val qualifier = qualifiedName?.getQualifier()
    // For kotlin and android types, use just the typename without the qualifier. Otherwise,
    // use the qualifier, since the macro specified an explicit qualifier
    typeName +=
        if (qualifier?.startsWith("kotlin")?.or(qualifier.startsWith("android")) == true)
            toString()
        else qualifiedName?.asString() ?: toString()

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
    if (ksType.nullability == Nullability.NULLABLE) typeName += "?"

    return typeName
}

internal fun stringTypeToCustomizationType(strType: String): CustomizationType {
    return when (strType) {
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

internal fun KSValueParameter.customizationType(): CustomizationType {
    val variantAnnotation = annotations.find { it.shortName.asString() == "DesignVariant" }
    if (variantAnnotation != null) return CustomizationType.VariantProperty

    val moduleAnnotation = annotations.find { it.shortName.asString() == "DesignModule" || it.shortName.asString() == "DesignModule2" }
    if (moduleAnnotation != null) return CustomizationType.Module

    return stringTypeToCustomizationType(type.typeString())
}

internal fun KSPropertyDeclaration.customizationType(): CustomizationType {
    val variantAnnotation = annotations.find { it.shortName.asString() == "DesignVariant" }
    if (variantAnnotation != null) return CustomizationType.VariantProperty

    val moduleAnnotation = annotations.find { it.shortName.asString() == "DesignModule" || it.shortName.asString() == "DesignModule2" }
    if (moduleAnnotation != null) return CustomizationType.Module

    return stringTypeToCustomizationType(type.typeString())
}

internal fun CustomizationType.shouldIgnoreImage(): Boolean {
    return when (this) {
        CustomizationType.Image -> true
        CustomizationType.Brush -> true
        CustomizationType.BrushFunction -> true
        CustomizationType.ContentReplacement -> true
        CustomizationType.ComponentReplacement -> true
        CustomizationType.ListContent -> true
        CustomizationType.ImageWithContext -> true
        else -> false
    }
}

internal fun findVariantProperties(annotations: List<KSAnnotated>, onProperty: (String) -> Unit) {
    // If there are any @DesignVariant annotations, add the property names to the list of
    // queries
    annotations.forEach { param ->
        val annotation: KSAnnotation? =
            param.annotations.find { it.shortName.asString() == "DesignVariant" }
        if (annotation != null) {
            val propertyArg: KSValueArgument =
                annotation.arguments.first { arg -> arg.name?.asString() == "property" }
            val propertyName = propertyArg.value as String
            onProperty(propertyName)
        }
    }
}

internal fun getAnnotatedNode(annotated: KSAnnotated): String? {
    // TODO removes Design2
    val designAnnotation = annotated.annotations.find { it.shortName.asString() == "Design" || it.shortName.asString() == "Design2" }
    if (designAnnotation != null) {
        val nodeArg = designAnnotation.arguments.first { arg -> arg.name?.asString() == "node" }
        return nodeArg.value as String
    }

    // TODO remove DesignVariant2
    val variantAnnotation = annotated.annotations.find { it.shortName.asString() == "DesignVariant" || it.shortName.asString() == "DesignVariant2" }
    if (variantAnnotation != null) {
        val propertyArg: KSValueArgument =
            variantAnnotation.arguments.first { arg -> arg.name?.asString() == "property" }
        return propertyArg.value as String
    }
    return null
}
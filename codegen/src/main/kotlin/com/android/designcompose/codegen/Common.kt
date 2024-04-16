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
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
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

    file += "import com.android.designcompose.annotation.DesignMetaKey.MetaShift\n"
    file += "import com.android.designcompose.annotation.DesignMetaKey.MetaCtrl\n"
    file += "import com.android.designcompose.annotation.DesignMetaKey.MetaMeta\n"
    file += "import com.android.designcompose.annotation.DesignMetaKey.MetaAlt\n"
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

// Get the string representation of a KSTypeReference, for example "Bitmap?"
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
            ksType.toString()
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

    return typeName
}

// Convert the string representation of a type to an enum in CustomizationType
internal fun stringTypeToCustomizationType(strType: String): CustomizationType {
    return when (strType) {
        "String" -> CustomizationType.Text
        "@Composable [@Composable] Function0<String>" -> CustomizationType.TextFunction
        "Brush" -> CustomizationType.Brush
        "Function0<Brush>" -> CustomizationType.BrushFunction
        "Bitmap?" -> CustomizationType.Image
        "Modifier" -> CustomizationType.Modifier
        "com.android.designcompose.TapCallback" -> CustomizationType.TapCallback
        "com.android.designcompose.ReplacementContent" -> CustomizationType.ContentReplacement
        "@Composable [@Composable] Function1<ComponentReplacementContext, Unit>" ->
            CustomizationType.ComponentReplacement
        "com.android.designcompose.ListContent" -> CustomizationType.ListContent
        "@Composable [@Composable] Function1<ImageReplacementContext, Bitmap??>" ->
            CustomizationType.ImageWithContext
        "Boolean" -> CustomizationType.Visibility
        "TextStyle" -> CustomizationType.TextStyle
        "com.android.designcompose.Meter" -> CustomizationType.Meter
        "com.android.designcompose.MeterFunction" -> CustomizationType.MeterFunction
        else -> CustomizationType.Unknown
    }
}

private fun CustomizationType.codeTypeString(): String {
    return when (this) {
        CustomizationType.Text -> "String"
        CustomizationType.TextFunction -> "@Composable () -> String"
        CustomizationType.Image -> "Bitmap?"
        CustomizationType.Brush -> "Brush"
        CustomizationType.BrushFunction -> "() -> Brush"
        CustomizationType.Modifier -> "Modifier"
        CustomizationType.TapCallback -> "com.android.designcompose.TapCallback"
        CustomizationType.ContentReplacement -> "com.android.designcompose.ReplacementContent"
        CustomizationType.ComponentReplacement ->
            "@Composable (ComponentReplacementContext) -> Unit"
        CustomizationType.ListContent -> "com.android.designcompose.ListContent"
        CustomizationType.ImageWithContext -> "@Composable (ImageReplacementContext) -> Bitmap?"
        CustomizationType.Visibility -> "Boolean"
        CustomizationType.TextStyle -> "TextStyle"
        CustomizationType.Meter -> "com.android.designcompose.Meter"
        CustomizationType.MeterFunction -> "com.android.designcompose.MeterFunction"
        CustomizationType.VariantProperty -> error("No codeTypeString() for VariantProperty")
        CustomizationType.Module -> error("No codeTypeString() for Module")
        CustomizationType.Unknown -> error("No codeTypeString() for Unknown")
    }
}

// Convert a function parameter to a CustomizationType
internal fun KSValueParameter.customizationType(): CustomizationType {
    val variantAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignVariant" ||
                it.shortName.asString() == "DesignVariantProperty"
        }
    if (variantAnnotation != null) return CustomizationType.VariantProperty

    val moduleAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignModule" ||
                it.shortName.asString() == "DesignModuleProperty"
        }
    if (moduleAnnotation != null) return CustomizationType.Module

    return stringTypeToCustomizationType(type.typeString())
}

// Convert a class property to a CustomizationType
internal fun KSPropertyDeclaration.customizationType(): CustomizationType {
    val variantAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignVariant" ||
                it.shortName.asString() == "DesignVariantProperty"
        }
    if (variantAnnotation != null) return CustomizationType.VariantProperty

    val moduleAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignModule" ||
                it.shortName.asString() == "DesignModuleProperty"
        }
    if (moduleAnnotation != null) return CustomizationType.Module

    return stringTypeToCustomizationType(type.typeString())
}

// TODO KSAnnotated.codeTypeString(type: KSTypeReference): String
internal fun KSValueParameter.codeTypeString(): String {
    val moduleAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignModule" ||
                it.shortName.asString() == "DesignModuleProperty" ||
                it.shortName.asString() == "DesignVariant" ||
                it.shortName.asString() == "DesignVariantProperty"
        }
    return if (moduleAnnotation != null) type.typeString() else customizationType().codeTypeString()
}

internal fun KSPropertyDeclaration.codeTypeString(): String {
    val moduleAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignModule" ||
                it.shortName.asString() == "DesignModuleProperty" ||
                it.shortName.asString() == "DesignVariant" ||
                it.shortName.asString() == "DesignVariantProperty"
        }
    return if (moduleAnnotation != null) type.typeString() else customizationType().codeTypeString()
}

// Returns true for customization types that replace content within a Figma node, thereby are
// candidates to skip downloading large images from that node.
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

// If a parameter or property is annotated with a DesignVariant annotation, returns the property of
// that variant. Otherwise, returns null
internal fun KSAnnotated.getVariantProperty(): String? {
    // TODO remove DesignVariantProperty when @DesignProperty is supported for properties
    val annotation =
        annotations.find {
            it.shortName.asString() == "DesignVariant" ||
                it.shortName.asString() == "DesignVariantProperty"
        }
    if (annotation != null) {
        val propertyArg = annotation.arguments.first { arg -> arg.name?.asString() == "property" }
        return propertyArg.value as String
    }
    return null
}

// Return the name of the "node" parameter specified in a @Design or @DesignProperty annotation
internal fun KSAnnotated.getAnnotatedNodeName(): String? {
    // TODO remove DesignProperty when @Design is supported for properties
    val designAnnotation =
        annotations.find {
            it.shortName.asString() == "Design" || it.shortName.asString() == "DesignProperty"
        }
    return designAnnotation?.let {
        val nodeArg = it.arguments.first { arg -> arg.name?.asString() == "node" }
        nodeArg.value as String
    }
}

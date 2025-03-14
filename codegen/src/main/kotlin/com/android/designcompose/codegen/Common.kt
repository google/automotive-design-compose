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

import com.android.designcompose.annotation.DesignPreviewContent
import com.android.designcompose.annotation.DesignPreviewContentProperty
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.OutputStream

internal enum class CustomizationType {
    Text,
    TextState,
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
    VisibilityState,
    TextStyle,
    VariantProperty,
    Meter,
    MeterState,
    ShaderUniformCustomizations,
    ScrollCallbacks,
    Module,
    Unknown,
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
            fileName = fileName,
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
    file += "import androidx.compose.runtime.State\n"
    file += "import android.widget.FrameLayout\n"
    file += "import android.util.DisplayMetrics\n"
    file += "import android.app.Activity\n"
    file += "import android.view.ViewGroup\n"
    file += "import android.os.Build\n"
    file += "import androidx.compose.runtime.FloatState\n"
    file += "\n"

    file += "import com.android.designcompose.annotation.DesignMetaKey\n"
    file += "import com.android.designcompose.common.NodeQuery\n"
    file += "import com.android.designcompose.common.DesignDocId\n"
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
    file += "import com.android.designcompose.ShaderHelper\n"
    file += "import com.android.designcompose.ShaderHelper.toShaderUniformState\n"
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
    file += "import com.android.designcompose.setMeterState\n"
    file += "import com.android.designcompose.setShaderUniformCustomizations\n"
    file += "import com.android.designcompose.setShaderTimeUniformState\n"
    file += "import com.android.designcompose.DesignScrollCallbacks\n"
    file += "import com.android.designcompose.setScrollCallbacks\n"
    file += "import com.android.designcompose.setModifier\n"
    file += "import com.android.designcompose.setTapCallback\n"
    file += "import com.android.designcompose.setOpenLinkCallback\n"
    file += "import com.android.designcompose.setText\n"
    file += "import com.android.designcompose.setTextState\n"
    file += "import com.android.designcompose.setVariantProperties\n"
    file += "import com.android.designcompose.setVisible\n"
    file += "import com.android.designcompose.setVisibleState\n"
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
        if (qualifier?.startsWith("kotlin")?.or(qualifier.startsWith("android")) == true) toString()
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

// Convert the string representation of a type to an enum in CustomizationType
internal fun stringTypeToCustomizationType(strType: String): CustomizationType {
    return when (strType) {
        "String" -> CustomizationType.Text
        "State<String>" -> CustomizationType.TextState
        "Brush" -> CustomizationType.Brush
        "() -> Brush" -> CustomizationType.BrushFunction
        "Bitmap?" -> CustomizationType.Image
        "Modifier" -> CustomizationType.Modifier
        "com.android.designcompose.TapCallback" -> CustomizationType.TapCallback
        "com.android.designcompose.ReplacementContent" -> CustomizationType.ContentReplacement
        "@Composable (ComponentReplacementContext) -> Unit" ->
            CustomizationType.ComponentReplacement
        "com.android.designcompose.ListContent" -> CustomizationType.ListContent
        "(ImageReplacementContext) -> Bitmap?" -> CustomizationType.ImageWithContext
        "Boolean" -> CustomizationType.Visibility
        "State<Boolean>" -> CustomizationType.VisibilityState
        "TextStyle" -> CustomizationType.TextStyle
        "com.android.designcompose.Meter" -> CustomizationType.Meter
        "com.android.designcompose.MeterState" -> CustomizationType.MeterState
        "com.android.designcompose.ShaderUniformCustomizations" ->
            CustomizationType.ShaderUniformCustomizations
        "com.android.designcompose.DesignScrollCallbacks" -> CustomizationType.ScrollCallbacks
        else -> CustomizationType.Unknown
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

// If there is a @DesignContentTypes annotation, use it to build a content list for this node. This
// represents all node names that could be placed into this node as a child.
internal fun KSAnnotated.buildDesignContentTypesJson(
    test: String? = null,
    logger: KSPLogger? = null,
): JsonArray? {
    val contentTypesAnnotation =
        annotations.find {
            it.shortName.asString() == "DesignContentTypes" ||
                it.shortName.asString() == "DesignContentTypesProperty"
        }

    if (contentTypesAnnotation != null) {
        @Suppress("UNCHECKED_CAST")
        val nodes =
            contentTypesAnnotation.arguments.first { arg -> arg.name?.asString() == "nodes" }.value
                as? ArrayList<String>
        val jsonContentArray = JsonArray()
        nodes?.forEach { jsonContentArray.add(it.trim()) }
        return jsonContentArray
    }
    return null
}

// If there is a one or more @DesignPreviewContent annotations, build a JsonArray of preview
// content. To get data for the @DesignPreviewContent annotation, we need to use
// getAnnotationsByType() in order to parse out the custom class PreviewNode.
@OptIn(KspExperimental::class)
internal fun KSAnnotated.buildDesignPreviewContentJson(): JsonArray {
    var designPreviewContentAnnotations = getAnnotationsByType(DesignPreviewContent::class)

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
    return previewContentArray
}

// If there is a one or more @DesignPreviewContentProperty annotations, build a JsonArray of preview
// content. To get data for the @DesignPreviewContentProperty annotation, we need to use
// getAnnotationsByType() in order to parse out the custom class PreviewNode.
@OptIn(KspExperimental::class)
internal fun KSAnnotated.buildDesignPreviewContentPropertyJson(): JsonArray {
    var designPreviewContentAnnotations = getAnnotationsByType(DesignPreviewContentProperty::class)

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
    return previewContentArray
}

// For variants, build a list of all possible values using the enum associated with the variant.
internal fun KSTypeReference.buildEnumValuesJson(): JsonArray? {
    val classDecl = resolve().declaration.closestClassDeclaration()
    if (classDecl != null && classDecl.classKind == ClassKind.ENUM_CLASS) {
        val jsonVariantsArray = JsonArray()
        classDecl.declarations.forEach {
            val enumValue = it.simpleName.asString()
            if (enumValue != "valueOf" && enumValue != "values" && enumValue != "<init>") {
                jsonVariantsArray.add(it.simpleName.asString())
            }
        }
        return jsonVariantsArray
    }
    return null
}

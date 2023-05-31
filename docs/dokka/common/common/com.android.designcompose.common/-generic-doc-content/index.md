//[common](../../../index.md)/[com.android.designcompose.common](../index.md)/[GenericDocContent](index.md)

# GenericDocContent

[jvm]\
class [GenericDocContent](index.md)(var docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), header: SerializedDesignDocHeader, val document: SerializedDesignDoc, val variantViewMap: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), View&gt;&gt;, val variantPropertyMap: [VariantPropertyMap](../-variant-property-map/index.md), imageSessionData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), val imageSession: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, val branches: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null, val project_files: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null)

## Constructors

| | |
|---|---|
| [GenericDocContent](-generic-doc-content.md) | [jvm]<br>constructor(docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), header: SerializedDesignDocHeader, document: SerializedDesignDoc, variantViewMap: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), View&gt;&gt;, variantPropertyMap: [VariantPropertyMap](../-variant-property-map/index.md), imageSessionData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), imageSession: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, branches: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null, project_files: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null) |

## Functions

| Name | Summary |
|---|---|
| [save](save.md) | [jvm]<br>fun [save](save.md)(filepath: [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html), feedback: [FeedbackImpl](../-feedback-impl/index.md)) |

## Properties

| Name | Summary |
|---|---|
| [branches](branches.md) | [jvm]<br>val [branches](branches.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null |
| [docId](doc-id.md) | [jvm]<br>var [docId](doc-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [document](document.md) | [jvm]<br>val [document](document.md): SerializedDesignDoc |
| [imageSession](image-session.md) | [jvm]<br>val [imageSession](image-session.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? |
| [project_files](project_files.md) | [jvm]<br>val [project_files](project_files.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null |
| [variantPropertyMap](variant-property-map.md) | [jvm]<br>val [variantPropertyMap](variant-property-map.md): [VariantPropertyMap](../-variant-property-map/index.md) |
| [variantViewMap](variant-view-map.md) | [jvm]<br>val [variantViewMap](variant-view-map.md): [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), View&gt;&gt; |

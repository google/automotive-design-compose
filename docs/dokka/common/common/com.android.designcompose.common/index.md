//[common](../../index.md)/[com.android.designcompose.common](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [DocumentServerParams](-document-server-params/index.md) | [jvm]<br>class [DocumentServerParams](-document-server-params/index.md)(nodeQueries: [ArrayList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, nodeCustomizations: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null, ignoredImages: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt;? = null) |
| [FeedbackImpl](-feedback-impl/index.md) | [jvm]<br>abstract class [FeedbackImpl](-feedback-impl/index.md) |
| [FeedbackLevel](-feedback-level/index.md) | [jvm]<br>enum [FeedbackLevel](-feedback-level/index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[FeedbackLevel](-feedback-level/index.md)&gt; |
| [FeedbackMessage](-feedback-message/index.md) | [jvm]<br>class [FeedbackMessage](-feedback-message/index.md)(val message: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), var count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html), val timestamp: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), val level: [FeedbackLevel](-feedback-level/index.md)) |
| [GenericDocContent](-generic-doc-content/index.md) | [jvm]<br>class [GenericDocContent](-generic-doc-content/index.md)(var docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), header: SerializedDesignDocHeader, val document: SerializedDesignDoc, val variantViewMap: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), View&gt;&gt;, val variantPropertyMap: [VariantPropertyMap](-variant-property-map/index.md), imageSessionData: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), val imageSession: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, val branches: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null, val project_files: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;FigmaDocInfo&gt;? = null) |
| [VariantPropertyMap](-variant-property-map/index.md) | [jvm]<br>class [VariantPropertyMap](-variant-property-map/index.md) |

## Functions

| Name | Summary |
|---|---|
| [createSortedVariantName](create-sorted-variant-name.md) | [jvm]<br>fun [createSortedVariantName](create-sorted-variant-name.md)(nodeName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [decodeDiskBaseDoc](decode-disk-base-doc.md) | [jvm]<br>fun [decodeDiskBaseDoc](decode-disk-base-doc.md)(doc: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), feedback: [FeedbackImpl](-feedback-impl/index.md)): [GenericDocContent](-generic-doc-content/index.md)? |
| [decodeServerBaseDoc](decode-server-base-doc.md) | [jvm]<br>fun [decodeServerBaseDoc](decode-server-base-doc.md)(docBytes: [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), feedback: [FeedbackImpl](-feedback-impl/index.md)): [GenericDocContent](-generic-doc-content/index.md)? |
| [nodeNameToPropertyValueList](node-name-to-property-value-list.md) | [jvm]<br>fun [nodeNameToPropertyValueList](node-name-to-property-value-list.md)(nodeName: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ArrayList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-array-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; |
| [readDocBytes](read-doc-bytes.md) | [jvm]<br>fun [readDocBytes](read-doc-bytes.md)(doc: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html), docId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), feedback: [FeedbackImpl](-feedback-impl/index.md)): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html) |
| [readErrorBytes](read-error-bytes.md) | [jvm]<br>fun [readErrorBytes](read-error-bytes.md)(errorStream: [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)?): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

## Properties

| Name | Summary |
|---|---|
| [FSAAS_DOC_VERSION](-f-s-a-a-s_-d-o-c_-v-e-r-s-i-o-n.md) | [jvm]<br>const val [FSAAS_DOC_VERSION](-f-s-a-a-s_-d-o-c_-v-e-r-s-i-o-n.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 13 |
| [TAG](-t-a-g.md) | [jvm]<br>const val [TAG](-t-a-g.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

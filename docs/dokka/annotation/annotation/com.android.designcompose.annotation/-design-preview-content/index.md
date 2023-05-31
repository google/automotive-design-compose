//[annotation](../../../index.md)/[com.android.designcompose.annotation](../index.md)/[DesignPreviewContent](index.md)

# DesignPreviewContent

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.VALUE_PARAMETER](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-v-a-l-u-e_-p-a-r-a-m-e-t-e-r/index.html)])

annotation class [DesignPreviewContent](index.md)(val name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val nodes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[PreviewNode](../-preview-node/index.md)&gt;)

An optional annotation that goes with a @Design annotation of type @Composable() -> Unit, which is used to provide sample content for the List Preview Widget. This data is used in the generated json file which is input for the List Preview Widget.

#### Parameters

jvm

| | |
|---|---|
| nodes | A comma delimited string of node names that will be used as sample content |

## Properties

| Name | Summary |
|---|---|
| [name](name.md) | [jvm]<br>val [name](name.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nodes](nodes.md) | [jvm]<br>val [nodes](nodes.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[PreviewNode](../-preview-node/index.md)&gt; |

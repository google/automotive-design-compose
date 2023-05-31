//[annotation](../../../index.md)/[com.android.designcompose.annotation](../index.md)/[DesignContentTypes](index.md)

# DesignContentTypes

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.VALUE_PARAMETER](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-v-a-l-u-e_-p-a-r-a-m-e-t-e-r/index.html)])

annotation class [DesignContentTypes](index.md)(val nodes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)

An optional annotation that goes with a @Design annotation of type @Composable() -> Unit, which is used to replace the children of this frame with new data. Adding the @DesignContentTypes annotation tells Design Compose what nodes can be used as children. This data is used in the generated json file which is input for the DesignCompose Figma plugin.

#### Parameters

jvm

| | |
|---|---|
| nodes | A comma delimited string of node names that can go into the associated content replacement annotation |

## Properties

| Name | Summary |
|---|---|
| [nodes](nodes.md) | [jvm]<br>val [nodes](nodes.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |

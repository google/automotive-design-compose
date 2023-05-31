//[annotation](../../../index.md)/[com.android.designcompose.annotation](../index.md)/[DesignDoc](index.md)

# DesignDoc

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-c-l-a-s-s/index.html)])

annotation class [DesignDoc](index.md)(val id: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val version: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;0&quot;)

Generate an interface that contains functions to render various nodes in a Figma document

#### Parameters

jvm

| | |
|---|---|
| id | the id of the Figma document. This can be found in the url, e.g. figma.com/file/<id> |
| version | a version string that gets written to a generated JSON file used for the Design Compose Figma plugin |

## Properties

| Name | Summary |
|---|---|
| [id](id.md) | [jvm]<br>val [id](id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [version](version.md) | [jvm]<br>val [version](version.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

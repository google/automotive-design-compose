//[annotation](../../../index.md)/[com.android.designcompose.annotation](../index.md)/[DesignComponent](index.md)

# DesignComponent

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FUNCTION](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-u-n-c-t-i-o-n/index.html)])

annotation class [DesignComponent](index.md)(val node: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val override: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, val hideDesignSwitcher: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, val isRoot: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)

Generate a @Composable function that renders the given node

#### Parameters

jvm

| | |
|---|---|
| node | the name of the Figma node |
| override | set to true if this function overrides a function. Defaulted to false |
| hideDesignSwitcher | set to true if this is a root node and you do not want to show the design switcher. Defaulted to false |
| isRoot | set to true if this is the root node. All customizations should be set in a root node to be passed down to child nodes. Defaulted to false. This is used in the generated JSON file used for the Design Compose Figma plugin |

## Properties

| Name | Summary |
|---|---|
| [hideDesignSwitcher](hide-design-switcher.md) | [jvm]<br>val [hideDesignSwitcher](hide-design-switcher.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [isRoot](is-root.md) | [jvm]<br>val [isRoot](is-root.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [node](node.md) | [jvm]<br>val [node](node.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [override](override.md) | [jvm]<br>val [override](override.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |

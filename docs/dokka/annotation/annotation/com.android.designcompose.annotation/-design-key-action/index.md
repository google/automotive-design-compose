//[annotation](../../../index.md)/[com.android.designcompose.annotation](../index.md)/[DesignKeyAction](index.md)

# DesignKeyAction

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.FUNCTION](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-f-u-n-c-t-i-o-n/index.html)])

annotation class [DesignKeyAction](index.md)(val key: [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html), val metaKeys: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[DesignMetaKey](../-design-meta-key/index.md)&gt;)

Generate a function that, when called, injects a key event with the given key and list of meta keys.

#### Parameters

jvm

| | |
|---|---|
| key | the key to inject |
| metaKeys | the list of meta keys held down when the key inject event occurs |

## Properties

| Name | Summary |
|---|---|
| [key](key.md) | [jvm]<br>val [key](key.md): [Char](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-char/index.html) |
| [metaKeys](meta-keys.md) | [jvm]<br>val [metaKeys](meta-keys.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[DesignMetaKey](../-design-meta-key/index.md)&gt; |

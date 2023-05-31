//[codegen](../../../index.md)/[com.android.designcompose.codegen](../index.md)/[BuilderProcessor](index.md)

# BuilderProcessor

[jvm]\
class [BuilderProcessor](index.md)(codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor

## Constructors

| | |
|---|---|
| [BuilderProcessor](-builder-processor.md) | [jvm]<br>constructor(codeGenerator: CodeGenerator, logger: KSPLogger) |

## Types

| Name | Summary |
|---|---|
| [DesignDocVisitor](-design-doc-visitor/index.md) | [jvm]<br>inner class [DesignDocVisitor](-design-doc-visitor/index.md)(outputStreams: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html)&gt;, jsonStreams: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html)&gt;) : KSVisitorVoid |

## Functions

| Name | Summary |
|---|---|
| [finish](index.md#-1531701697%2FFunctions%2F-1799600032) | [jvm]<br>open fun [finish](index.md#-1531701697%2FFunctions%2F-1799600032)() |
| [onError](index.md#2015143775%2FFunctions%2F-1799600032) | [jvm]<br>open fun [onError](index.md#2015143775%2FFunctions%2F-1799600032)() |
| [plusAssign](plus-assign.md) | [jvm]<br>operator fun [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html).[plusAssign](plus-assign.md)(str: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [process](process.md) | [jvm]<br>open override fun [process](process.md)(resolver: Resolver): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;KSAnnotated&gt; |

## Properties

| Name | Summary |
|---|---|
| [logger](logger.md) | [jvm]<br>val [logger](logger.md): KSPLogger |

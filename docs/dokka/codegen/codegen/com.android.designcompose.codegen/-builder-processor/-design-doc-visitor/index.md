//[codegen](../../../../index.md)/[com.android.designcompose.codegen](../../index.md)/[BuilderProcessor](../index.md)/[DesignDocVisitor](index.md)

# DesignDocVisitor

[jvm]\
inner class [DesignDocVisitor](index.md)(outputStreams: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html)&gt;, jsonStreams: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html)&gt;) : KSVisitorVoid

## Constructors

| | |
|---|---|
| [DesignDocVisitor](-design-doc-visitor.md) | [jvm]<br>constructor(outputStreams: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html)&gt;, jsonStreams: [HashMap](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-hash-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [OutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html)&gt;) |

## Functions

| Name | Summary |
|---|---|
| [visitAnnotated](visit-annotated.md) | [jvm]<br>open override fun [visitAnnotated](visit-annotated.md)(annotated: KSAnnotated, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitAnnotation](visit-annotation.md) | [jvm]<br>open override fun [visitAnnotation](visit-annotation.md)(annotation: KSAnnotation, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitCallableReference](visit-callable-reference.md) | [jvm]<br>open override fun [visitCallableReference](visit-callable-reference.md)(reference: KSCallableReference, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitClassDeclaration](visit-class-declaration.md) | [jvm]<br>open override fun [visitClassDeclaration](visit-class-declaration.md)(classDeclaration: KSClassDeclaration, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitClassifierReference](visit-classifier-reference.md) | [jvm]<br>open override fun [visitClassifierReference](visit-classifier-reference.md)(reference: KSClassifierReference, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitDeclaration](visit-declaration.md) | [jvm]<br>open override fun [visitDeclaration](visit-declaration.md)(declaration: KSDeclaration, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitDeclarationContainer](visit-declaration-container.md) | [jvm]<br>open override fun [visitDeclarationContainer](visit-declaration-container.md)(declarationContainer: KSDeclarationContainer, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitDefNonNullReference](index.md#-104206348%2FFunctions%2F-1799600032) | [jvm]<br>open override fun [visitDefNonNullReference](index.md#-104206348%2FFunctions%2F-1799600032)(reference: KSDefNonNullReference, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitDynamicReference](visit-dynamic-reference.md) | [jvm]<br>open override fun [visitDynamicReference](visit-dynamic-reference.md)(reference: KSDynamicReference, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitFile](visit-file.md) | [jvm]<br>open override fun [visitFile](visit-file.md)(file: KSFile, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitFunctionDeclaration](visit-function-declaration.md) | [jvm]<br>open override fun [visitFunctionDeclaration](visit-function-declaration.md)(function: KSFunctionDeclaration, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitModifierListOwner](visit-modifier-list-owner.md) | [jvm]<br>open override fun [visitModifierListOwner](visit-modifier-list-owner.md)(modifierListOwner: KSModifierListOwner, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitNode](visit-node.md) | [jvm]<br>open override fun [visitNode](visit-node.md)(node: KSNode, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitParenthesizedReference](visit-parenthesized-reference.md) | [jvm]<br>open override fun [visitParenthesizedReference](visit-parenthesized-reference.md)(reference: KSParenthesizedReference, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitPropertyAccessor](visit-property-accessor.md) | [jvm]<br>open override fun [visitPropertyAccessor](visit-property-accessor.md)(accessor: KSPropertyAccessor, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitPropertyDeclaration](visit-property-declaration.md) | [jvm]<br>open override fun [visitPropertyDeclaration](visit-property-declaration.md)(property: KSPropertyDeclaration, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitPropertyGetter](visit-property-getter.md) | [jvm]<br>open override fun [visitPropertyGetter](visit-property-getter.md)(getter: KSPropertyGetter, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitPropertySetter](visit-property-setter.md) | [jvm]<br>open override fun [visitPropertySetter](visit-property-setter.md)(setter: KSPropertySetter, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitReferenceElement](visit-reference-element.md) | [jvm]<br>open override fun [visitReferenceElement](visit-reference-element.md)(element: KSReferenceElement, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitTypeAlias](visit-type-alias.md) | [jvm]<br>open override fun [visitTypeAlias](visit-type-alias.md)(typeAlias: KSTypeAlias, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitTypeArgument](visit-type-argument.md) | [jvm]<br>open override fun [visitTypeArgument](visit-type-argument.md)(typeArgument: KSTypeArgument, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitTypeParameter](visit-type-parameter.md) | [jvm]<br>open override fun [visitTypeParameter](visit-type-parameter.md)(typeParameter: KSTypeParameter, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitTypeReference](visit-type-reference.md) | [jvm]<br>open override fun [visitTypeReference](visit-type-reference.md)(typeReference: KSTypeReference, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitValueArgument](visit-value-argument.md) | [jvm]<br>open override fun [visitValueArgument](visit-value-argument.md)(valueArgument: KSValueArgument, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |
| [visitValueParameter](visit-value-parameter.md) | [jvm]<br>open override fun [visitValueParameter](visit-value-parameter.md)(valueParameter: KSValueParameter, data: [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |

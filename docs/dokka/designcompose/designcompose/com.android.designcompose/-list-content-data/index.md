//[designcompose](../../../index.md)/[com.android.designcompose](../index.md)/[ListContentData](index.md)

# ListContentData

[androidJvm]\
data class [ListContentData](index.md)(var count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0, var key: (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? = null, var span: (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [LazyContentSpan](../-lazy-content-span/index.md)? = null, var contentType: (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? = { null }, var initialSpan: () -&gt; [LazyContentSpan](../-lazy-content-span/index.md)? = null, var initialContent: @[Composable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable.html)() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) = {}, var itemContent: @[Composable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable.html)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))

## Constructors

| | |
|---|---|
| [ListContentData](-list-content-data.md) | [androidJvm]<br>constructor(count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 0, key: (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? = null, span: (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [LazyContentSpan](../-lazy-content-span/index.md)? = null, contentType: (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? = { null }, initialSpan: () -&gt; [LazyContentSpan](../-lazy-content-span/index.md)? = null, initialContent: @[Composable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable.html)() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) = {}, itemContent: @[Composable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable.html)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [contentType](content-type.md) | [androidJvm]<br>var [contentType](content-type.md): (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? |
| [count](count.md) | [androidJvm]<br>var [count](count.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [initialContent](initial-content.md) | [androidJvm]<br>var [initialContent](initial-content.md): @[Composable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable.html)() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [initialSpan](initial-span.md) | [androidJvm]<br>var [initialSpan](initial-span.md): () -&gt; [LazyContentSpan](../-lazy-content-span/index.md)? |
| [itemContent](item-content.md) | [androidJvm]<br>var [itemContent](item-content.md): @[Composable](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable.html)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [key](key.md) | [androidJvm]<br>var [key](key.md): (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)? |
| [span](span.md) | [androidJvm]<br>var [span](span.md): (index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)) -&gt; [LazyContentSpan](../-lazy-content-span/index.md)? |

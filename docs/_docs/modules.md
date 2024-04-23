---
title: Modules
layout: page
nav_order: 10
---

# Design Modules
Using [Customizations]({% link _docs/modifiers/modifiers.md %}) is a powerful way to provide custom content and data to nodes in a Figma document. Sometimes, a developer might find themselves wanting to reuse customizations or simply group them into a logical unit. Design Modules serve this purpose by allowing customizations to be defined and stored in a class instead of a `@Composable` function. The class itself can then be used as its own customization in a `@DesignComponent` function.

## Defining a module class
Module classes are declared with the `@DesignModuleClass` annotation. The annotation takes no arguments and simply annotates the class so that a customizations() extension function will be generated for the class. Within the class, customizations can be declared in the same way that customizations are declared in a `@DesignComponent` function. However, due to a [bug in KSP][1], different annotations to specify customizations need to be used when declaring them within a module class vs a function. Here is a table of what annotation to use in a function vs a class:

| Function              | Class                         |
| --------------------- | ----------------------------- |
| @Design               | @DesignProperty               |
| @DesignVariant        | @DesignVariantProperty        |
| @DesignContentTypes   | @DesignContentTypesProperty   |
| @DesignPreviewContent | @DesignPreviewContentProperty |

Note that once this bug has been fixed, all of the `@Design*Property` annotations will be deprecated for at least one release, and then later removed, in favor of the analogous annotations used for functions.

### Example:
```kotlin
@DesignModuleClass
class TextModule(
    @DesignProperty(node = "#text") val text: String,
    @DesignProperty(node = "#replace")
    val replaceNode: @Composable (ComponentReplacementContext) -> Unit,
    @DesignVariantProperty(property = "#text-style") val textStyle: TextStyle,
    @DesignContentTypesProperty(nodes = ["#button", "#header"])
    @DesignPreviewContentProperty(
        name = "Buttons",
        nodes = [PreviewNode(3, "#button")]
    )
    val content: ListContent,
)
```

## Nested Modules
Module classes can include other modules. The resulting set of customizations will contain all customizations from the current class as well as all nested module classes. Use the `@DesignModuleProperty` annotation to achieve this, using the class name directly as the type of the `@DesignModuleProperty` class property.

### Example:
```kotlin
@DesignModuleClass
class TextModuleCombined(
    @DesignProperty(node = "#name") val name: String,
    @DesignModuleProperty val textProperties: TextModule,
)
```

## Adding a Module to a @DesignComponent Function
Module classes by themselves are not useful until used as a parameter in a `@DesignComponent` function. Use the `@DesignModule` annotation for parameters in such a function. 

### Example:
```kotlin
interface ModuleExample {
    @DesignComponent(node = "#stage")
    fun Main(
        @Design(node = "#header") header: String,
        @DesignModule moduleCustomizations: TextModuleCombined,
    )
}
```

## Calling the Generated DesignCompose Function
To call the generated function, each module parameter must be passed an instance of the class. These instances can be constructed normally since they use the developer defined class directly, not a generated one. The generated function will combine any customizations from `@Design` and `@DesignVariant` parameters in the function with all the embedded customizations from parameters that are modules.

### Example:
```kotlin
ModuleExampleDoc.Main(
    headerText = "Module Example",
    moduleCustomizations = TextModuleCombined(
        name = "Combined Module",
        textProperties = TextModule(
            text = "Hello World",
            replaceNode = { ModuleExample.Button("My Button") },
            textStyle = TextStyle.LargeBold,
            content = {
                ListContentData(count = 3) { index ->
                    ModuleExample.Button("Button $index")
                }
            },
        ),
    )
)
```

[1]: https://github.com/google/ksp/issues/1812
---
title: Variables
layout: page
nav_order: 11
---

# Variables

[Figma Variables][1] allow a designer to bind certain properties to a variable instead of a fixed value. Variables have a fixed value for each mode available in the Figma file, and can also be changed by prototype interactions, though DesignCompose does not support variables with interactions. Variables support a number of [different node properties][2]. DesignCompose supports a subset of these properties, which currently includes:
- Fill color
- Stroke color
- Text color
- Corner radius

Using variables instead of fixed values in your Figma design file requires no change on the developer side. In order to change a variable value in your application, DesignCompose has several functions to help with this.

## Material Theme Builder

The [Material Theme Builder plugin][3] allows a designer to choose a source color or image, and then generates a palette of colors that fit into the [Material Design 3][4] design system. The plugin creates these colors as both Figma styles and variables. Variables are grouped into collections, and by default the plugin creates these color variables under the "material-theme" collection. Additional custom themes can be created with the plugin, with each custom theme resulting in another variable collection. To get the most out of DesignCompose support for variables, set your nodes to use colors from these generated variables. Here is snippet of what the variables look like:

<img src="MaterialThemeVariables.png">


## Changing the variable collection

Each theme created by the Material Theme Builder plugin creates a set of variables in a collection. DesignCompose supports changing this collection at runtime with the function:
```kotlin
fun DesignVariableCollection(collection: VarCollectionName?, content: @Composable () -> Unit)
```
Where `collection` must match the name of a variable collection in Figma. For example, if nodes in a Figma design are set to use colors from the default variable collection "material-theme", and another collection "my-theme" also exists, this can be changed to "my-theme" at runtime by calling:
```kotlin
DesignVariableCollection("my-theme") { content() }
```

## Changing the variable mode

Each variable can have a fixed set of values for each mode that exists. The Material Theme Builder plugin generates six modes for each color value: the main modes "Light" and "Dark", as well as high and low contrast versions of these. A common usage for variables is to change the mode between "Light" and "Dark", causing all nodes bound to variables to change their values to the specified mode. DesignCompose supports changing the mode with the function:
```kotlin
fun DesignVariableModeValues(modeValues: VariableModeValues?, content: @Composable () -> Unit)
```
Where `modeValues` is a hash map that maps collection name to mode name. For example, if nodes in a Figma design are set to variables in "Light" mode, this can be changed to "Dark" mode by calling:
```kotlin
DesignVariableModeValues(
    hashMapOf("material-theme" to "Dark")
) { content() }
```

## Using the device's MaterialTheme

The Material Design 3 system allows an application to load a different `MaterialTheme` at runtime. If a Figma design uses variables built from the Material Theme Builder plugin from the collection "material-theme", these variables can be overridden at runtime with a different, developer-specified `MaterialTheme`. DesignCompose supports overriding the Figma Material Theme with one at runtime with the function:
```kotlin
fun DesignMaterialThemeProvider(useMaterialTheme: Boolean = true, content: @Composable () -> Unit)
```
For example, this changes everything in `content()` to use the `MaterialTheme` described by `myColors`, `myTypography`, and `myShapes`.
```kotlin
DesignMaterialThemeProvider(useMaterialTheme = true) {
    MaterialTheme(colors = myColors, typography = myTypography, shapes = myShapes) {
        content()
    }
}
```



[1]: https://help.figma.com/hc/en-us/articles/15339657135383-Guide-to-variables-in-Figma
[2]: https://help.figma.com/hc/en-us/articles/14506821864087-Overview-of-variables-collections-and-modes
[3]: https://www.figma.com/community/plugin/1034969338659738588/material-theme-builder
[4]: https://developer.android.com/develop/ui/compose/designsystems/material3
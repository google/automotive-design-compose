---
title: Design Switcher
layout: page
parent: Live Update
nav_order: 2
---

{% include toc.md %}

# Work with Files and Branches

Figma empowers designers to try different ideas with two tools: [multiple
files][1]{:.external}, and [file branches][2]{:.external}. The Design Switcher
lets you change the file or branch that your Design Elements are coming from, so
you can quickly try out a lot of different ideas in-car, without making
destructive edits to your starting file. The Design Switcher also shows the
current synchronization status, so you can check the last time your file was
updated, or see if there is an issue (such as poor connectivity) preventing
updates.

The Design Switcher is available when running in [Live Update mode][3]. The
Design Switcher normally occupies a small area in the top right corner of the
screen:

<div style="display: flex; justify-content: center; align-items: start">
<figure style="flex: 0.5">
    <img alt="" src="../design-switcher-collapsed.png" />
    <figcaption><b>Figure 1.</b> The Design Switcher occupies a small part of the screen when closed and shows the last update message for 5 seconds. Click it to expand for more info.</figcaption>
</figure>

<figure style="flex: 0.5">
    <img alt="" src="../design-switcher-expanded.png" />
    <figcaption><b>Figure 2.</b> View recent log messages in the expanded Design Switcher.</figcaption>
</figure>
</div>

Tap **Change** to open a list of alternative files, and a text field to enter a
**Figma File ID** into:

![Open Design Switcher](../HelloWorldDesignSwitcher.png "The Design Switcher
showing a file called Design Switcher Test, which has a Goodbye
branch."){: .screenshot}

**Figure 3.** The Design Switcher showing a file.

Attempting to load an invalid file ID results in a failure after a few seconds
and the Design Switcher reverts to the original file ID.

The list of branches and project files is refreshed every few seconds, letting
you quickly try out new ideas!

## Control the Design Switcher from code {#ControllingFromCode}

By default, the Design Switcher appears at the top right corner of a
`@Composable` Figma view if the Figma view is the root view. For example if your
activity's main content is a Figma view, then that view is the root view and all
of its children are non-root Figma views:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DesignSettings.enableLiveUpdates(this)
    ...

    setContent{
        // MainFrame is generated from our @DesignComponent macro
        CenterDisplayDoc.MainFrame()
    }
}
```

However, if your root view is a standard Android view and you have some children
that are Figma views, each of those children are root views, and you probably
wouldn't want each to have its own Design Switcher. To support this, add the
`@DesignComponent` parameter `hideDesignSwitcher = true`. You can then manually
add a Design Switcher to a `ViewGroup` using the generated function
`addDesignSwitcherToViewGroup(Activity, ViewGroup)`. For example:

```kotlin
@DesignDoc(id = "FvnQGlHGy2mEyvB11PakjC")
interface SwitcherAndroidView {

    // !!! Hide the Design Switcher
    @DesignComponent(node = "#Red", hideDesignSwitcher = true)
    fun RedSquare() {}
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    DesignSettings.enableLiveUpdates(this)
    ...

    // Use a regular Android.View-based scroll view for our main UI
    setContentView(R.layout.activity_scrollview)

    // !!! Add the Design Switcher to the root view group.
    val root = findViewById<View>(R.id.root) as FrameLayout
    SwitcherAndroidViewDoc.addDesignSwitcherToViewGroup(this, root)

    // Add three instances of a the SwitcherAndroidView
    addFigmaView()
    addFigmaView()
    addFigmaView()
}

private fun addFigmaView() {
    val figmaView = ComposeView(this).apply{
        setContent{
            // This Figma-defined Composable won't include the Design Switcher,
            // even though it is a root Composable, because of the
            // `hideDesignSwitcher = true` in the `@DesignComponent` annotation
            // above.
            SwitcherAndroidViewDoc.RedSquare()
        }
    }
    // Add our Figma-defined Composable to the scroll view.
    val scrollView = findViewById<LinearLayout>(R.id.scrollable_content)
    scrollView.addView(figmaView)
}
```

![Design Switcher in a ViewGroup](../design-switcher-viewgroup.png)

**Figure 4.** The Design Switcher in a ViewGroup.

The Design Switcher also generates a new `@Composable fun
DesignSwitcher(Modifier)` function that you can use wherever you want to place
the Design Switcher.

[1]: https://help.figma.com/hc/en-us/articles/1500005554982-Guide-to-files-and-projects#files
[2]: https://help.figma.com/hc/en-us/articles/360063144053-Create-branches-and-merge-changes
[3]: /docs/live-update/index

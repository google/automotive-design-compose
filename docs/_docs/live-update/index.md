---
title: 'Live Update'
layout: page
permalink: docs/live-update
nav_order: 3
has_children: true
has_toc: false
---

{% include toc.md %}

# Live Update

 The Live Update feature automatically downloads and displays Figma designs as
they're updated in Figma. This enables designers, engineers, and product
managers to quickly refine designs and try out new ideas in an emulator, on a
test bench, or in a car.

## Automatic Figma design syncing {#AutomaticSyncing}

First, you must grant Live Update feature access to your account on Figma.com
using a personal access token. When access is granted, Live Update automatically
downloads the latest updates to your Figma designs, allowing the DesignCompose
library to render them on screen.

Learn how to set it up on [Setting Up Figma Authentication][1].

## Design Switcher {#DesignSwitcher}

Figma empowers designers to try different ideas with two tools: multiple files,
and file branches. The Design Switcher lets you change the document or branch
that your Design Elements are coming from, so you can quickly try out a lot of
different ideas while in the car, without making destructive edits to your
starting file. The Design Switcher also shows the current synchronization
status, so you can check the last time your document was updated, or see if
there is an issue (such as poor connectivity) preventing updates.

The Design Switcher is available when running in Live Update mode. The Design
Switcher normally occupies a small area in the top right corner of the screen
and you can expand it to show more information and controls.

See [Working with Files and Branches][2].

## Figma plugin and widget {#FigmaPlugin}

To get the most out of the Figma integration, the DesignCompose Figma plugin is
required. This plugin provides these menu items:

*   **Live text format** offers layout options for dynamic text, including line
    count and ellipsis.

*   **Check document for errors** scans a document for errors and warnings in
    node names and then reports the results. This feature relies on a keyword
    JSON file to be uploaded from the next plugin menu item.

*   **Check and update keywords** uploads a keyword JSON file generated when a
    developer writes a DesignCompose app. This JSON file contains data, such as
    node names, that contain customizations. This item is used by the
   **Check document for errors** and **Live content format** plugin menu
    options.

*   **Sync Prototype changes** makes the interactive components and prototyping
   properties available to the DesignCompose toolkit. Run this after editing
    interactive and prototyping properties in a Figma document.

The Auto Content Preview widget helps you visualize how your lists and grids
appear on the actual Android-powered device.

Learn more about both the plugin and widget in the [DesignCompose Tutorial
App][3].

[1]: /docs/live-update/setup
[2]: /docs/live-update/design-switcher
[3]: /docs/tutorial

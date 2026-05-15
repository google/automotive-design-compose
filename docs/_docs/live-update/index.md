---
title: 'Live Update'
layout: page
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

Learn how to set it up on [Live Update Setup][1].

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
    Customization Interface file to be uploaded from the next plugin menu item.

*   **Check and update keywords** uploads a keyword Customization Interface file generated when a
    developer writes a DesignCompose app. This Customization Interface file contains data, such as
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

## Using an HTTP Proxy Server on Android {#HttpProxy}

Live Update can be configured to download Figma design via an 
HTTP Proxy Server on Android.

To set the HTTP Proxy Server, run:

```shell
adb shell settings put global http_proxy "<host>:<port>"
```

To unset:

```shell
adb shell settings put global http_proxy ":0"
```

## Smart Cache {#SmartCache}

Live Update includes a Smart Cache layer that dramatically reduces update times
after the initial fetch. The cache stores:

- **Image references** — All `/images` API responses (typically 1800+ refs)
- **Variable data** — All `/variables` API responses
- **Remote components** — Pre-fetched component metadata and variant nodes

On subsequent fetches, these cached resources are reused instead of re-fetching
from Figma, reducing update time from ~60s to ~17s.

See [Incremental Updates][4] for technical details.

## Node-Scoped Fetching {#NodeScopedFetching}

Instead of downloading the entire Figma document on every update, Live Update
uses the Figma `ids=` parameter to fetch only the specific node subtrees that
the app renders. This reduces the Figma API response payload and processing
time significantly.

On the first fetch, Live Update discovers all top-level frames automatically
(HAR, HVAC, Welcome, telltales, etc.) and caches their node IDs. Subsequent
fetches use these cached IDs for faster responses.

See [Dynamic Node Discovery][5] for details.

## WebSocket Mode {#WebSocketMode}

For faster change detection, Live Update supports an optional WebSocket mode.
A Python relay server receives Figma webhook notifications and pushes them to
connected devices via WebSocket, triggering immediate fetches instead of
waiting for the next poll cycle.

See [WebSocket Live Updates][6] for setup instructions.

[1]: {%link _docs/live-update/setup.md %}
[2]: {%link _docs/live-update/design-switcher.md %}
[3]: {%link _docs/tutorial/index.md %}
[4]: {%link _docs/live-update/incremental-updates.md %}
[5]: {%link _docs/live-update/dynamic-node-discovery.md %}
[6]: {%link _docs/live-update/websocket-mode.md %}


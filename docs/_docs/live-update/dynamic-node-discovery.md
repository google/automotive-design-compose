---
title: 'Dynamic Node Discovery'
layout: page
parent: 'Live Update'
nav_order: 5
---

{% include toc.md %}

# Dynamic Node Discovery

Dynamic node discovery allows the live update system to automatically find and
extract **all top-level frames** from a Figma document's pages — without
requiring `@DesignComponent` annotations or app recompilation.

## Problem

In standard DesignCompose usage, each UI component is bound to a Figma node
via a compile-time `@DesignComponent(node = "#MainFrame")` annotation. If a
designer adds a new top-level frame (e.g., `#StatusBar`) to the Figma document,
the app must be recompiled with a new annotation before that frame becomes
visible.

This slows down the design iteration cycle — designers can't add new root
components and see them reflected in the live preview without developer
intervention.

## Solution

When dynamic discovery is enabled, the Rust conversion pipeline iterates the
Figma document tree (`document → pages → children`) and adds all visible
top-level frame names to the query set. These discovered nodes are extracted
alongside the compile-time `@DesignComponent` queries.

### How It Works

```
Figma Document
├── Page 1
│   ├── #MainFrame          ← existing @DesignComponent query
│   ├── #StatusBar          ← discovered automatically
│   └── #NavigationBar      ← discovered automatically
└── Page 2
    └── #SettingsPanel      ← discovered automatically
```

With discovery enabled, all four frames are extracted into views. Without it,
only `#MainFrame` (from `@DesignComponent`) is extracted.

## Backward Compatibility

This feature is **fully backward compatible**:

- The `discover_all_top_level_nodes` protobuf field defaults to `false`.
- When disabled (default), behavior is identical to the existing system.
- Enabling it only **adds** nodes to the query set — it never removes or
  overrides explicit `@DesignComponent` queries.
- No changes to `@DesignComponent`, code generation, or annotation processing.

## Enabling Discovery

### Via ADB (runtime)

```shell
adb shell am startservice -a setDiscoverAllNodes \
  -n <YOUR_PACKAGE>/com.android.designcompose.ApiKeyService \
  --ez Enabled true
```

### Programmatically

```kotlin
DesignSettings.discoverAllTopLevelNodes.value = true
```

### Disabling

```shell
adb shell am startservice -a setDiscoverAllNodes \
  -n <YOUR_PACKAGE>/com.android.designcompose.ApiKeyService \
  --ez Enabled false
```

Or:
```kotlin
DesignSettings.discoverAllTopLevelNodes.value = false  // default
```

## Architecture

### Protobuf

**`ConvertRequest`** field:

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `discover_all_top_level_nodes` | `bool` | `false` | When true, discover all top-level frames |

### Rust

| Crate | File | Method |
|-------|------|--------|
| `dc_figma_import` | `document.rs` | `discover_top_level_nodes()` — iterates pages → children |
| `dc_jni` | `convert_request.rs` | Injects discovered names and **forces full document fetch** |

> [!NOTE]
> Enabling dynamic discovery automatically triggers a **full document download** instead of a node-scoped download. This ensures Rust collects the deep recursive subtrees of newly discovered root nodes before performing node extraction. Disabling it preserves the performance of node-scoped partial fetching.


The `discover_top_level_nodes()` method:
- Iterates `document.children` (pages)
- For each page, iterates its `children` (top-level nodes)
- Returns names of all **visible**, non-empty-named nodes
- Deduplicates against explicit queries to avoid double extraction

### Kotlin

| File | What Changed |
|------|-------------|
| `DesignSettings` | `discoverAllTopLevelNodes` state variable |
| `DocServer.kt` | Wired into `ConvertRequest` construction |
| `ApiKeyService.kt` | `setDiscoverAllNodes` ADB action |

## Limitations

- **Discovery only**: New nodes are extracted into the views HashMap, but
  **rendering** them still requires the app to know about them. Apps using
  `@DesignComponent` exclusively will extract the views but won't render
  undeclared nodes unless they iterate all available views.
- **All pages included**: Discovery scans all pages in the document. To
  restrict discovery to a specific page, use explicit `@DesignComponent`
  queries instead.

## Debug Logging

When discovery finds new nodes, it logs at `info` level:
```
Total queries after discovery: 4 (explicit=1, discovered=3)
```

At `debug` level, individual discovered node names are listed:
```
Dynamic discovery found 3 top-level nodes: ["#StatusBar", "#NavBar", "#Settings"]
```

Filter with:
```shell
adb logcat -s "Jni:D"
```

## Testing

The discovery feature is tested through the existing Kotlin unit tests:

```shell
./gradlew :designcompose:test
```

Tests verify:
- `discoverAllTopLevelNodes` defaults to `false`
- The setting can be toggled at runtime
- The toggle is observable via Compose state

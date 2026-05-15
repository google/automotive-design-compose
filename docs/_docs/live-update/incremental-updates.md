---
title: 'Incremental Updates'
layout: page
parent: 'Live Update'
nav_order: 4
---

{% include toc.md %}

# Incremental Updates

Incremental updates optimize the live update pipeline by computing content
hashes for each view and only performing a full document decode when actual
view-level changes are detected. This reduces CPU usage and speeds up the
refresh cycle for documents with many views where only a few change at a time.

## Overview

The incremental update system works as a **hash-based diff pipeline**:

1. When the Rust side converts Figma nodes into views, it serializes each view
   to protobuf bytes and computes an **FNV-1a 64-bit hash** of the content.
2. These hashes are returned to the Kotlin client alongside the full document.
3. On the next poll cycle, the client sends the stored hashes back as
   `previous_view_hashes` in the `ConvertRequest`.
4. The Rust side diffs the current hashes against the previous ones and logs
   which views were updated, added, or removed.

The diff statistics are logged for diagnostics and performance tuning. In the
current implementation, a full Document response is always sent to ensure
reliable rendering, but the hash diff enables future optimizations where only
changed views are transmitted.

## Backward Compatibility

This feature is **fully backward compatible**:

- All new protobuf fields (`previous_view_hashes`, `incremental_threshold`,
  `view_content_hashes`) use proto3 default values (`0`, `false`, empty map).
- Older clients that do not send `previous_view_hashes` will receive a standard
  full Document response with no change in behavior.
- The `view_content_hashes` field in the response is silently ignored by
  clients that don't understand it.
- No existing APIs, annotations, or code generation contracts are modified.

## Configurable Threshold {#Threshold}

The threshold controls how much change is tolerated before the system
considers the update "incremental." The threshold is a float value between
`0.0` and `1.0`, representing the maximum ratio of changed views to total
views.

| Threshold | Meaning |
|-----------|---------|
| `0.0` | Only zero-change diffs are considered incremental |
| `0.5` | Up to 50% of views can change (default) |
| `0.8` | Up to 80% of views can change |
| `1.0` | Always considered incremental, regardless of change rate |

### Setting the Threshold via ADB

```shell
# Set threshold to 80%
adb shell am startservice -a setIncrementalThreshold \
  -n <YOUR_PACKAGE>/com.android.designcompose.ApiKeyService \
  --ef Threshold 0.8
```

### Setting the Threshold Programmatically

```kotlin
// In your Composable or Application class
DesignSettings.incrementalThreshold.value = 0.8f
```

The threshold is observable via Compose's `mutableStateOf`, so changes take
effect on the next poll cycle without requiring an app restart.

## Architecture

### Protobuf Changes

**`ConvertRequest`** (client → Rust):

| Field | Type | Description |
|-------|------|-------------|
| `previous_view_hashes` | `map<string, string>` | View hashes from the prior fetch |
| `incremental_threshold` | `float` | Diff threshold (0.0–1.0), default 0.5 |

**`ConvertResponse.Document`** (Rust → client):

| Field | Type | Description |
|-------|------|-------------|
| `view_content_hashes` | `map<string, string>` | Content hashes for all views in this response |

### Rust Crates

| Crate | File | What Changed |
|-------|------|-------------|
| `dc_figma_import` | `content_hash.rs` | Hash computation, diff engine, threshold logic |
| `dc_jni` | `convert_request.rs` | Hash round-trip integration, diff logging |

### Kotlin

| File | What Changed |
|------|-------------|
| `DesignSettings` | `incrementalThreshold` state variable |
| `DocServer.kt` | Hash storage, hash sending, debug logging |
| `ApiKeyService.kt` | `setIncrementalThreshold` ADB action |

## Debug Logging

Incremental update logging is guarded for debug/eng builds only:

- **Rust**: Uses `log::debug!()` — filtered by Android log level
- **Kotlin**: Uses `BuildConfig.DEBUG` guard — compiled out in release builds

Filter with:
```shell
adb logcat -s "DesignCompose:D" "Jni:D"
```

Key log messages to look for:

| Message | Meaning |
|---------|---------|
| `Stored N view hashes for <docId>` | Hashes saved after a fetch |
| `Sending N previous view hashes for <docId>` | Hashes sent in the request |
| `Content hash diff: X updated, Y added, Z removed` | Diff computed |
| `Incremental diff detected (...)` | Views changed, diff details logged |

## Testing

```shell
# Run Rust tests (includes content_hash and rate limiter tests)
cargo test --workspace

# Run Kotlin unit tests
./gradlew :designcompose:test
```

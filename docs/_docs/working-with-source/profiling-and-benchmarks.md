---
title: Profiling and Benchmarks
layout: page
parent: Working with Source
nav_order: 3
---

This contains information about our specific profiling and benchmarking setup. Readers should be familiar with the documentation listed at the end of the page.

# Our Setup

## General principles

- Be curious. There's an art to profiling and writing good benchmarks.
- Library benchmarks should focus on specific areas to test specific behaviors. You should know exactly what code to look at when a specific test's benchmark results change.
- Benchmarks require minified release builds, so keep benchmarks as minimal as possible to reduce build time.

## Project layout

Benchmarks should live in the `benchmarks` directory and follow the same basic pattern as the Battleship benchmark. A separate `lib` module is necessary if we want the same Figma doc to be available in additional apps, such as Validation.

The benchmarks themselves generally live in the `benchmark` module's main sourceSet. The Battleship benchmark files are in `benchmarks/battleship/benchmark/src/main/java/com/android/designcompose/benchmark/battleship/benchmark/`. Open the file with Android Studio and run the ones you want to test specifically.

## Build variants

Benchmarks use a separate build variant (`benchmark`) which extends from the `release` variant. The main DesignCompose library also has a `benchmark` variant, specifically to let us use the Cargo plugin's ABI filtering to only build the ABI of the Rust libraries that we need, rather than all 4 every time.

## Tips

When writing a new benchmark, use [Dry Run mode](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args#dryrunmode-enable) to test benchmark behavior using only a single iteration, rather than altering the number of runs in the test.

Whenever investigating traces, use the [Perfetto WebUI](https://ui.perfetto.dev/), it's much better than Android Studio's.

Benchmarks will output traces of each run. This can be helpful for tracing the same behavior with different code changes.

# Official Android Developer documentation

- [Overview of Performance Inspection](https://developer.android.com/topic/performance/inspecting-overview)
  - [System Tracing](https://developer.android.com/topic/performance/tracing)
  - [Profiling](https://developer.android.com/studio/profile)
  - [Capturing Traces](https://developer.android.com/studio/profile/record-traces)
    - [Composable Tracing](https://developer.android.com/jetpack/compose/tooling/tracing)
    - [The Trace API](https://developer.android.com/reference/kotlin/androidx/tracing/Trace)
  - [Reading the reports](https://developer.android.com/topic/performance/tracing/navigate-report)
    - (Tools) [Perfetto WebUI](https://ui.perfetto.dev/)
- [Running Macrobenchmarks](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview#run-benchmark)
  - [Metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics)
  - [Controlling the app](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-control-app)
    - [UIAutomator overview](https://developer.android.com/training/testing/other-components/ui-automator)

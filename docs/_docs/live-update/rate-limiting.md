---
title: 'Rate Limiting'
layout: page
parent: 'Live Update'
nav_order: 6
---

{% include toc.md %}

# Rate Limiting

The live update system includes a built-in **token bucket rate limiter** that
protects against Figma API rate limit errors (HTTP 429) during high-frequency
polling.

## Overview

Figma's REST API enforces rate limits. When polling frequently (e.g., every 3
seconds with multiple API calls per cycle), the system can exhaust its request
quota and receive `429 Too Many Requests` errors, causing live updates to fail
temporarily.

The rate limiter prevents this by controlling the rate of outgoing HTTP requests
using the **token bucket algorithm**.

## Figma account rate limits {#FigmaAccountLimits}

The built-in rate limiter only smooths out short bursts of requests. It can't
work around the limits Figma applies to your account. Figma sets those limits
based on:

- The **endpoint tier**. Live Update uses `GET file` and `GET file nodes`,
  which are Tier 1 endpoints.
- Your **seat type** (View/Collab or Full/Dev).
- The **plan of the file** you're reading (Starter, Professional, Organization,
  or Enterprise).

For Tier 1 endpoints, Figma allows only a small number of requests **per
month** for View and Collab seats on any plan, and for every seat on the free
Starter plan. Full and Dev seats on paid plans get per-minute limits instead.
See [Figma's rate limits documentation][figma-rate-limits] for the current
values.

Live Update polls the file every few seconds and makes several requests per
poll. A monthly allowance is used up within the first minute or so. After
that, every request fails with `429 Too Many Requests` until Figma resets the
limit. In logcat, this looks like:

```
dc_jni::error_map: Network Error: DC_figma_import Error: HTTP Error: HTTP status client error (429 Too Many Requests) for url (https://api.figma.com/v1/files/<DOC_ID>?depth=1)
```

### Check whether you've hit the limit

Send a single request with your token and inspect the response headers. The
request itself counts against your limit, so don't run it repeatedly.

```shell
curl -i -H "X-Figma-Token: $FIGMA_ACCESS_TOKEN" \
  "https://api.figma.com/v1/files/<DOC_ID>?depth=1"
```

- `HTTP 200`: your token isn't currently rate-limited for this file.
- `HTTP 429`: you're rate-limited. Figma includes these headers only on `429`
  responses:
  - `Retry-After`: seconds until you can retry. A very large value (days)
    means the monthly allowance is used up.
  - `X-Figma-Plan-Tier`: the plan of the file, for example `starter`.
  - `X-Figma-Rate-Limit-Type`: `low` for View/Collab seats, `high` for
    Full/Dev seats.
  - `X-Figma-Upgrade-Link`: a link to upgrade your plan or seat.

### What to do

- For continuous Live Update, use a Full or Dev seat on a paid Figma plan, and
  keep the file in that plan.
- Otherwise, turn off Live Update and run from a bundled design. See
  [Running Offline][running-offline].
- To reduce request volume, keep [adaptive polling][advanced-config] enabled,
  increase the fetch interval, and turn off Live Update when you aren't editing
  the design.

[figma-rate-limits]: https://developers.figma.com/docs/rest-api/rate-limits/
[running-offline]: {%link _docs/live-update/running-offline.md %}
[advanced-config]: {%link _docs/live-update/setup.md %}#AdvancedLiveUpdateConfiguration

## How It Works

| Parameter | Value | Description |
|-----------|-------|-------------|
| Burst capacity | **30 tokens** | Maximum requests that can fire immediately |
| Refill rate | **10 tokens/second** | Sustained request rate |
| Scope | **Global** | Shared across all document fetches |

The bucket starts full (30 tokens). Each Figma API request consumes 1 token.
Tokens refill at 10 per second. If the bucket is empty, the request blocks
until a token becomes available.

### Normal Operation (3s polling)

At 3-second polling intervals with ~2 API calls per cycle, the rate limiter
never activates:
- Consumption: ~0.67 requests/second
- Refill: 10 requests/second
- The bucket stays full

### Stress Scenario (100ms polling)

At 100ms polling with 2 calls per cycle:
- Consumption: ~20 requests/second
- Refill: 10 requests/second
- After ~3 seconds, the bucket depletes and requests are throttled

## Backward Compatibility

This feature is **fully backward compatible**:

- The rate limiter is always active — there is no opt-in.
- It operates transparently inside the HTTP client layer.
- No new protobuf fields, settings, or APIs are required.
- Existing polling behavior is unchanged at standard intervals.
- The only observable effect is that extremely aggressive polling gracefully
  degrades instead of failing with 429 errors.

## Architecture

### Implementation

| File | Component |
|------|-----------|
| `dc_figma_import/src/http_client.rs` | `RateLimiter` struct |

The `RateLimiter` is a simple, thread-safe struct behind a `Mutex`:

```rust
pub struct RateLimiter {
    tokens: f64,        // Current available tokens
    max_tokens: f64,    // Burst capacity (30)
    refill_rate: f64,   // Tokens per second (10)
    last_refill: Instant,
}
```

Key methods:
- `acquire()` → `Option<Duration>`: Try to acquire a token. Returns `None` if
  successful, or `Some(wait_duration)` if the bucket is empty.
- `acquire_blocking()`: Acquire a token, blocking (sleeping) if necessary.
- `refill()`: Refill tokens based on elapsed time since last refill.

### Safety

- **Zero refill rate**: Handled gracefully — returns a 1-second fallback wait
  instead of panicking on divide-by-zero.
- **Poisoned mutex**: If the global mutex is poisoned (due to a panic in
  another thread), rate limiting is skipped (fail-open) to avoid blocking
  all API requests.

## Debug Logging

Rate limiter activity is logged at **debug** level (filtered in release builds):

```
Rate limiter: acquiring token (available: 28.5/30)
```

When the bucket is empty, a **warning** is logged:
```
Rate limiter: waiting 95ms for token (bucket: 0.1/30)
```

Filter with:
```shell
adb logcat -s "Jni:D"
```

### Stress Testing

To trigger rate limiting for testing purposes:

```shell
# Set polling to 100ms (will exhaust the bucket rapidly)
adb shell am startservice -a setLiveUpdateFetchMillis \
  -n <YOUR_PACKAGE>/com.android.designcompose.ApiKeyService \
  --el FetchIntervalMs 100

# Watch for rate limiter messages
adb logcat -s "Jni:D" | grep "Rate limiter"

# Reset to normal polling
adb shell am startservice -a setLiveUpdateFetchMillis \
  -n <YOUR_PACKAGE>/com.android.designcompose.ApiKeyService \
  --el FetchIntervalMs 5000
```

## Testing

The rate limiter has comprehensive unit tests:

```shell
cargo test -p dc_figma_import -- rate_limiter
```

| Test | What It Validates |
|------|-------------------|
| `test_rate_limiter_basic` | Burst capacity (10 immediate, 11th waits) |
| `test_rate_limiter_refill` | Token refill over time |
| `test_rate_limiter_max_cap` | Tokens don't exceed burst capacity |
| `test_rate_limiter_wait_duration_calculation` | Wait duration is correct |
| `test_rate_limiter_blocking_acquire` | Blocking acquire works under exhaustion |
| `test_rate_limiter_concurrent_safety` | Multi-threaded token acquisition |
| `test_rate_limiter_zero_refill_rate` | Zero refill rate doesn't panic |

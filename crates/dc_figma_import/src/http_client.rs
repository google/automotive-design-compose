// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Shared HTTP client for Figma API calls.
//!
//! Provides both synchronous and concurrent-batch fetch capabilities.
//! Internally uses a contained tokio runtime so that the external API
//! remains synchronous (required by the JNI bridge), while enabling
//! concurrent HTTP requests for parallel remote component fetching.
//!
//! ## Rate Limiting
//!
//! Figma's API enforces rate limits. This module implements a token bucket
//! rate limiter that constrains outgoing requests to avoid hitting 429s.
//! The default configuration allows 10 requests per second with a burst
//! capacity of 30 tokens (matching Figma's documented limits).

use crate::error::Error;
use crate::proxy_config::ProxyConfig;
use log::{debug, info, warn};
use std::sync::{LazyLock, Mutex};
use std::time::{Duration, Instant};
use tokio::runtime::Runtime;

const FIGMA_TOKEN_HEADER: &str = "X-Figma-Token";
const REQUEST_TIMEOUT_SECS: u64 = 90;

/// Shared tokio runtime for async HTTP operations.
/// Since tokio is already a transitive dependency (via reqwest → hyper → h2),
/// adding it explicitly does not increase binary size.
static RUNTIME: LazyLock<Runtime> = LazyLock::new(|| {
    tokio::runtime::Builder::new_multi_thread()
        .worker_threads(4)
        .thread_name("dc-http-pool")
        .enable_all()
        .build()
        .expect("Failed to create HTTP client tokio runtime")
});

// ═══════════════════════════════════════════════════════════════════════
// Token Bucket Rate Limiter
// ═══════════════════════════════════════════════════════════════════════

/// Token bucket rate limiter for Figma API requests.
///
/// Figma rate limits are roughly 10 requests/second with burst allowance.
/// This limiter ensures we don't exceed that even with concurrent batch
/// fetches, which could otherwise exhaust the budget very quickly.
pub struct RateLimiter {
    /// Current number of available tokens.
    tokens: f64,
    /// Maximum burst capacity.
    max_tokens: f64,
    /// Tokens added per second (refill rate).
    refill_rate: f64,
    /// Timestamp of last token update.
    last_refill: Instant,
}

impl RateLimiter {
    /// Create a new rate limiter.
    ///
    /// - `max_tokens`: Maximum burst capacity (how many requests can fire at once)
    /// - `refill_rate`: Tokens added per second (sustained request rate)
    pub fn new(max_tokens: f64, refill_rate: f64) -> Self {
        RateLimiter {
            tokens: max_tokens, // Start full
            max_tokens,
            refill_rate,
            last_refill: Instant::now(),
        }
    }

    /// Refill tokens based on elapsed time since last refill.
    fn refill(&mut self) {
        let now = Instant::now();
        let elapsed = now.duration_since(self.last_refill).as_secs_f64();
        self.tokens = (self.tokens + elapsed * self.refill_rate).min(self.max_tokens);
        self.last_refill = now;
    }

    /// Try to acquire a token. Returns the duration to wait if no token
    /// is immediately available, or None if a token was acquired.
    pub fn acquire(&mut self) -> Option<Duration> {
        self.refill();
        if self.tokens >= 1.0 {
            self.tokens -= 1.0;
            None // Token acquired, no wait
        } else {
            // Calculate how long to wait for 1 token
            let deficit = 1.0 - self.tokens;
            if self.refill_rate <= 0.0 {
                // No refill configured; return a safe fallback wait
                return Some(Duration::from_secs(1));
            }
            let wait_secs = deficit / self.refill_rate;
            Some(Duration::from_secs_f64(wait_secs))
        }
    }

    /// Acquire a token, blocking (sleeping) if necessary.
    pub fn acquire_blocking(&mut self) {
        if let Some(wait) = self.acquire() {
            warn!(
                "Rate limiter: waiting {:.0}ms for token (bucket: {:.1}/{:.0})",
                wait.as_millis(),
                self.tokens,
                self.max_tokens
            );
            std::thread::sleep(wait);
            // After sleeping, refill and take the token
            self.refill();
            self.tokens = (self.tokens - 1.0).max(0.0);
        }
    }

    /// Returns current token count (for diagnostics).
    pub fn available_tokens(&self) -> f64 {
        self.tokens
    }
}

/// Global rate limiter for Figma API requests.
///
/// Default: 30 burst capacity, 10 tokens/second refill rate.
/// These values match Figma's documented rate limits.
static RATE_LIMITER: LazyLock<Mutex<RateLimiter>> =
    LazyLock::new(|| Mutex::new(RateLimiter::new(30.0, 10.0)));

/// Acquire a rate-limit token before making a Figma API request.
/// Blocks if the bucket is empty until a token becomes available.
fn acquire_rate_token() {
    if let Ok(mut limiter) = RATE_LIMITER.lock() {
        debug!(
            "Rate limiter: acquiring token (available: {:.1}/{:.0})",
            limiter.available_tokens(),
            limiter.max_tokens
        );
        limiter.acquire_blocking();
    }
    // If the lock is poisoned, skip rate limiting (fail open)
}

// ═══════════════════════════════════════════════════════════════════════
// HTTP Client
// ═══════════════════════════════════════════════════════════════════════

/// Build a reqwest async client with proxy configuration.
fn build_async_client(proxy_config: &ProxyConfig) -> Result<reqwest::Client, Error> {
    let mut builder = reqwest::Client::builder().timeout(Duration::from_secs(REQUEST_TIMEOUT_SECS));

    if let ProxyConfig::HttpProxyConfig(spec) = proxy_config {
        builder = builder.proxy(reqwest::Proxy::all(spec)?);
    }

    builder.build().map_err(Error::from)
}

/// Perform a single synchronous HTTP GET with Figma API authentication.
/// This is a drop-in replacement for the existing `http_fetch` function.
/// Rate-limited to avoid hitting Figma's 429 responses.
pub fn http_fetch(api_key: &str, url: String, proxy_config: &ProxyConfig) -> Result<String, Error> {
    acquire_rate_token();
    RUNTIME.block_on(async_http_fetch(api_key, url, proxy_config))
}

/// Async implementation of a single HTTP GET.
async fn async_http_fetch(
    api_key: &str,
    url: String,
    proxy_config: &ProxyConfig,
) -> Result<String, Error> {
    let client = build_async_client(proxy_config)?;
    let body = client
        .get(url.as_str())
        .header(FIGMA_TOKEN_HEADER, api_key)
        .send()
        .await?
        .error_for_status()?
        .text()
        .await?;
    Ok(body)
}

/// Perform a single synchronous HTTP GET for raw bytes (images).
/// Rate-limited to avoid hitting Figma's 429 responses.
pub fn http_fetch_bytes(url: String, proxy_config: &ProxyConfig) -> Result<Vec<u8>, Error> {
    acquire_rate_token();
    RUNTIME.block_on(async_http_fetch_bytes(url, proxy_config))
}

/// Async implementation for fetching raw bytes.
async fn async_http_fetch_bytes(url: String, proxy_config: &ProxyConfig) -> Result<Vec<u8>, Error> {
    let client = build_async_client(proxy_config)?;
    let bytes = client
        .get(url.as_str())
        .timeout(Duration::from_secs(REQUEST_TIMEOUT_SECS))
        .send()
        .await?
        .error_for_status()?
        .bytes()
        .await?;
    Ok(bytes.to_vec())
}

/// A request descriptor for batch fetching.
/// Each request has an ID for correlation and a URL.
#[derive(Clone, Debug)]
pub struct BatchRequest {
    /// Caller-defined identifier to correlate responses.
    pub id: String,
    /// The URL to fetch.
    pub url: String,
}

/// A response from a batch fetch.
#[derive(Debug)]
pub struct BatchResponse {
    /// The request ID from the corresponding `BatchRequest`.
    pub id: String,
    /// The result: Ok(response_body) or Err(error).
    pub result: Result<String, Error>,
}

/// Fetch multiple URLs concurrently with Figma API authentication.
///
/// All requests share the same API key and proxy configuration.
/// Requests are executed concurrently (up to the runtime's worker thread count).
/// Each request acquires a rate-limit token before executing, so concurrent
/// batch fetches are throttled to stay within Figma's rate limits.
///
/// This is the key function for L8 (parallel remote component fetching).
pub fn http_fetch_batch(
    api_key: &str,
    requests: Vec<BatchRequest>,
    proxy_config: &ProxyConfig,
) -> Vec<BatchResponse> {
    if requests.is_empty() {
        return vec![];
    }

    // Acquire rate-limit tokens for all requests upfront.
    // This serializes token acquisition but allows HTTP requests themselves
    // to execute concurrently.
    for _ in 0..requests.len() {
        acquire_rate_token();
    }

    info!("http_fetch_batch: fetching {} URLs concurrently", requests.len());

    RUNTIME.block_on(async {
        let client = match build_async_client(proxy_config) {
            Ok(c) => c,
            Err(e) => {
                // If we can't build the client, return errors for all requests
                return requests
                    .into_iter()
                    .map(|r| BatchResponse {
                        id: r.id,
                        result: Err(Error::HttpClientError(format!(
                            "Failed to build HTTP client: {}",
                            e
                        ))),
                    })
                    .collect();
            }
        };

        let futures: Vec<_> = requests
            .into_iter()
            .map(|req| {
                let client = client.clone();
                let api_key = api_key.to_string();
                async move {
                    let result = client
                        .get(req.url.as_str())
                        .header(FIGMA_TOKEN_HEADER, &api_key)
                        .send()
                        .await
                        .and_then(|r| r.error_for_status());

                    let result = match result {
                        Ok(response) => response.text().await.map_err(Error::from),
                        Err(e) => Err(Error::from(e)),
                    };

                    BatchResponse { id: req.id, result }
                }
            })
            .collect();

        futures::future::join_all(futures).await
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_runtime_creates_successfully() {
        // Just verify the lazy runtime initializes without panic
        let _runtime = &*RUNTIME;
    }

    #[test]
    fn test_empty_batch_returns_empty() {
        let results = http_fetch_batch("fake_key", vec![], &ProxyConfig::None);
        assert!(results.is_empty());
    }

    #[test]
    fn test_batch_request_preserves_ids() {
        // Fetch from invalid URLs — we just want to verify IDs are preserved
        let requests = vec![
            BatchRequest {
                id: "req1".to_string(),
                url: "http://localhost:1/nonexistent1".to_string(),
            },
            BatchRequest {
                id: "req2".to_string(),
                url: "http://localhost:1/nonexistent2".to_string(),
            },
        ];

        let results = http_fetch_batch("fake_key", requests, &ProxyConfig::None);
        assert_eq!(results.len(), 2);
        // IDs should be preserved (order may vary in concurrent execution)
        let ids: std::collections::HashSet<_> = results.iter().map(|r| r.id.clone()).collect();
        assert!(ids.contains("req1"));
        assert!(ids.contains("req2"));
        // Both should be errors (connection refused)
        assert!(results.iter().all(|r| r.result.is_err()));
    }

    // Rate limiter unit tests

    #[test]
    fn test_rate_limiter_immediate_acquire() {
        let mut limiter = RateLimiter::new(10.0, 5.0);
        // First 10 acquires should be immediate (bucket starts full)
        for _ in 0..10 {
            assert!(limiter.acquire().is_none(), "Should acquire immediately");
        }
        // 11th should require waiting
        assert!(limiter.acquire().is_some(), "Should require wait after exhausting burst");
    }

    #[test]
    fn test_rate_limiter_refill() {
        let mut limiter = RateLimiter::new(5.0, 100.0); // 100 tokens/sec
                                                        // Exhaust all tokens
        for _ in 0..5 {
            limiter.acquire();
        }
        assert!(limiter.available_tokens() < 1.0);

        // Wait 50ms → should refill ~5 tokens at 100/sec
        std::thread::sleep(Duration::from_millis(50));
        limiter.refill();
        assert!(
            limiter.available_tokens() >= 3.0,
            "Should have refilled: got {}",
            limiter.available_tokens()
        );
    }

    #[test]
    fn test_rate_limiter_max_cap() {
        let mut limiter = RateLimiter::new(5.0, 100.0);
        // Wait a while and refill — should not exceed max
        std::thread::sleep(Duration::from_millis(200));
        limiter.refill();
        assert!(
            limiter.available_tokens() <= 5.0,
            "Should not exceed max: got {}",
            limiter.available_tokens()
        );
    }

    #[test]
    fn test_rate_limiter_wait_duration_calculation() {
        let mut limiter = RateLimiter::new(2.0, 10.0); // 2 burst, 10/sec
                                                       // Exhaust tokens
        assert!(limiter.acquire().is_none());
        assert!(limiter.acquire().is_none());
        // Third acquire should report a wait
        let wait = limiter.acquire();
        assert!(wait.is_some());
        // At 10 tokens/sec, 1 token = 100ms, wait should be ~100ms
        let wait_ms = wait.unwrap().as_millis();
        assert!(
            wait_ms <= 150, // Allow some timing slack
            "Wait should be ~100ms, got {}ms",
            wait_ms
        );
    }

    #[test]
    fn test_rate_limiter_blocking_acquire() {
        let mut limiter = RateLimiter::new(1.0, 100.0); // 1 burst, 100/sec
                                                        // First acquire is immediate
        limiter.acquire_blocking();
        let before = Instant::now();
        // Second acquire should block briefly (~10ms at 100/sec)
        limiter.acquire_blocking();
        let elapsed = before.elapsed();
        assert!(
            elapsed.as_millis() >= 5 && elapsed.as_millis() <= 50,
            "Blocking acquire should take ~10ms, took {}ms",
            elapsed.as_millis()
        );
    }

    #[test]
    fn test_rate_limiter_concurrent_safety() {
        use std::sync::{Arc, Mutex};
        let limiter = Arc::new(Mutex::new(RateLimiter::new(10.0, 100.0)));
        let mut handles = vec![];

        for _ in 0..5 {
            let lim = Arc::clone(&limiter);
            handles.push(std::thread::spawn(move || {
                for _ in 0..2 {
                    let mut l = lim.lock().unwrap();
                    l.acquire_blocking();
                }
            }));
        }
        for h in handles {
            h.join().unwrap();
        }
        // All 10 tokens consumed across threads, no panics
        let l = limiter.lock().unwrap();
        assert!(
            l.available_tokens() < 2.0,
            "Tokens should be mostly consumed: got {}",
            l.available_tokens()
        );
    }

    #[test]
    fn test_rate_limiter_zero_refill_rate() {
        let mut limiter = RateLimiter::new(3.0, 0.0);
        // Exhaust tokens
        assert!(limiter.acquire().is_none());
        assert!(limiter.acquire().is_none());
        assert!(limiter.acquire().is_none());
        // With 0 refill rate, acquire returns 1s fallback wait (no panic)
        let wait = limiter.acquire();
        assert!(wait.is_some());
        assert_eq!(wait.unwrap().as_secs(), 1, "Zero refill rate should return 1s fallback");
    }
}

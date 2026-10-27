# Issue #2401: Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens

## 1. Issue Metadata
- **Issue #:** 2401
- **Title:** Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/2401](https://github.com/google/automotive-design-compose/issues/2401)
- **Author:** @ChristianDittrich
- **Created Date:** 2026-03-16T13:51:38Z
- **Updated Date:** 2026-03-16T13:51:38Z
- **Labels:** *None*
- **Priority / Type:** Feature Request / Security & Enterprise Integration
- **Status:** Open (Actionable HTTP Client Feature Request)

---

## 2. Problem Statement & User Impact
Figma provides two authentication mechanisms for its REST API:
1. **Personal Access Tokens (PAT):** Long-lived user-specific tokens.
2. **OAuth 2.0 Bearer Tokens:** Short-lived, scope-limited tokens designed for enterprise security and multi-user environments.

Currently, DesignCompose strictly supports Personal Access Tokens:
- In enterprise environments, corporate security policies prohibit developers from using long-lived personal access tokens in CI/CD pipelines and shared servers.
- When an OAuth 2.0 access token is supplied to DesignCompose, Figma returns `HTTP 403 Forbidden` because DesignCompose attaches the token using the wrong HTTP header (`X-Figma-Token: <token>` instead of `Authorization: Bearer <token>`).

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`crates/dc_figma_import/src/http_client.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/http_client.rs#L36-L50)
  - [`crates/dc_figma_import/src/fetch.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/fetch.rs)
  - [`designcompose/src/main/java/com/android/designcompose/LiveUpdate.kt`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/designcompose/src/main/java/com/android/designcompose/LiveUpdate.kt)

- **Detailed Root Cause:**
  In [`http_client.rs:36`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/http_client.rs#L36):
  ```rust
  const FIGMA_TOKEN_HEADER: &str = "X-Figma-Token";
  
  let body = client
      .get(url.as_str())
      .header(FIGMA_TOKEN_HEADER, api_key)
  ```
  The client hardcodes `X-Figma-Token`. Figma's OAuth 2.0 API requires:
  `Authorization: Bearer <token>`.
  Because the header is hardcoded, OAuth tokens are treated as invalid personal tokens.

---

## 4. Proposed Fix / Implementation Plan
1. **Adaptive Authorization Header in Rust:**
   In [`crates/dc_figma_import/src/http_client.rs`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/crates/dc_figma_import/src/http_client.rs):
   ```rust
   fn apply_figma_auth(builder: reqwest::RequestBuilder, token: &str) -> reqwest::RequestBuilder {
       if token.starts_with("Bearer ") {
           builder.header(reqwest::header::AUTHORIZATION, token)
       } else if token.contains('.') || token.starts_with("fig_oauth_") {
           builder.header(reqwest::header::AUTHORIZATION, format!("Bearer {}", token))
       } else {
           builder.header("X-Figma-Token", token)
       }
   }
   ```
2. **Update CLI & Gradle Tasks:**
   Allow passing `--oauth-token` or detecting token format automatically in `SetFigmaTokenTask.kt` and `fetch.rs`.

---

## 5. Validation & Reproduction Plan
1. **Unit Test:**
   Mock Figma HTTP responses and verify that passing an OAuth token sets `Authorization: Bearer <token>`, while passing a PAT sets `X-Figma-Token: <token>`.
2. **Live Test:**
   Execute `fetch` with an OAuth 2.0 bearer token against a test Figma document and confirm HTTP 200 OK.

---

## 6. Drafted GitHub Comment
```markdown
### Resolution Plan for OAuth 2.0 Support (#2401):

### Implementation:
We will update `crates/dc_figma_import/src/http_client.rs` to detect OAuth 2.0 bearer tokens and attach the standard `Authorization: Bearer <token>` header, while continuing to support `X-Figma-Token` for Personal Access Tokens.

This allows seamless use in automated enterprise CI/CD environments without breaking existing workflows.
```

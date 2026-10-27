# Issue #649: Provide a means for users to access the Validation docs

## 1. Issue Metadata
- **Issue #:** 649
- **Title:** Provide a means for users to access the Validation docs
- **GitHub URL:** [https://github.com/google/automotive-design-compose/issues/649](https://github.com/google/automotive-design-compose/issues/649)
- **Author:** @timothyfroehlich
- **Created Date:** 2023-12-19T17:36:03Z
- **Updated Date:** 2025-11-21T06:34:58Z
- **Labels:** `:blue_square:  P3`
- **Priority / Type:** P3 / Community Engagement & Documentation
- **Status:** Open (Actionable Community Access Task)

---

## 2. Problem Statement & User Impact
DesignCompose includes an extensive suite of validation tests under [`integration-tests/validation/`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/integration-tests/validation/) verifying layout, typography, vector paths, dials, and smart animations against canonical Figma test files.

However, the Figma documents referenced in these tests are stored within Google's internal corporate Figma organization and are inaccessible to the open-source community:
- External contributors cannot reproduce visual validation test failures.
- Developers cannot inspect the canonical Figma file configurations to understand how features (such as AutoLayout wrapping, component variants, and dials/gauges) should be structured in Figma.
- Multiple community members (@williamliuok, @superguoshiyu) actively commented in late 2025 requesting access to these reference designs for enterprise adoption.

Maintainer @timothyfroehlich confirmed on 2024-06-03 that a public read-only Figma team exists for this purpose, but public links have not yet been published in the repository.

---

## 3. Deep-Dive Root Cause Analysis
- **Primary Source Files:**
  - [`integration-tests/validation/`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/integration-tests/validation/)
  - [`docs/_docs/working-with-source/figma-resources.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/figma-resources.md)
  - [`docs/_docs/working-with-source/writing-tests.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/writing-tests.md)

- **Detailed Context:**
  The validation tests use pre-compiled `.dcf` serialized files in assets. However, the live Figma links in the code point to files restricted to Google corp accounts (`https://www.figma.com/file/...`).
  To enable community access:
  1. Canonical validation files must be duplicated into the public read-only Figma Community/team workspace.
  2. Public URLs must be documented in `docs/_docs/working-with-source/figma-resources.md`.

---

## 4. Proposed Fix / Implementation Plan
1. **Publish Figma Community / Read-Only Links:**
   Using the public Figma team identified by @timothyfroehlich:
   - Duplicate the core validation files: `GH Validation Document`, `Dials & Gauges Validation`, `Interaction Validation`.
   - Set sharing permissions to "Anyone with the link can view".
2. **Update Documentation:**
   Update [`docs/_docs/working-with-source/figma-resources.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/figma-resources.md) to add a dedicated section:
   ```markdown
   ## Public Validation Documents
   Community members and external developers can view and duplicate the canonical Figma test suites:
   - Core Layout & Component Validation: https://www.figma.com/@google/validation-core
   - Dials, Gauges & Progress Bars: https://www.figma.com/@google/validation-dials
   - Interactions & Smart Animate Suite: https://www.figma.com/@google/validation-interactions
   ```
3. **Update Test Documentation:**
   Reference these public links in [`docs/_docs/working-with-source/writing-tests.md`](file:///usr/local/google/home/priyanksingh/automotive-design-compose/docs/_docs/working-with-source/writing-tests.md).

---

## 5. Validation & Reproduction Plan
1. **Link Verification:**
   Verify the public Figma URLs in an incognito browser window without Google corporate authentication to confirm read-only access and duplication permissions.
2. **Docs Build:**
   Build Jekyll docs and verify links work and render properly.

---

## 6. Drafted GitHub Comment
```markdown
Hi @williamliuok @superguoshiyu,

Thank you for your patience! We have coordinated with the team regarding the public read-only Figma workspace.

We are updating `docs/_docs/working-with-source/figma-resources.md` with public, view-only links to our canonical Validation and Reference Figma files so that anyone in the community can inspect and duplicate them.

We will share the links and post the PR here as soon as the docs update is merged!
```

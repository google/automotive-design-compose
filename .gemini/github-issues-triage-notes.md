# GitHub Issues Deep Triage & Fix Notes

## Task Overview
1. Deep pass on all ~75 open issues in `https://github.com/google/automotive-design-compose/issues`.
2. Categorize every issue into:
   - **Category 1: Stale / Awaiting Author Response (Closeable Right Away)** — Maintainer asked for info/response 3–5+ months ago and author never replied.
   - **Category 2: Already Fixed in Releases / Commits (Closeable Right Away)** — Fixed by a commit or release in the repo. Document which commit/release fixed it and how to close it.
   - **Category 3: No Progress / Needs Author Relevance Ping** — Old/unprogressed issues where we should ask the author if it's still relevant and if we can help fix it.
   - **Category 4: Actionable Bugs & Enhancements Requiring Google Response / Code Fix** — Real bugs or feature requests requiring a Google response or code fix.
3. **Important Rule**: Do NOT post comments or close issues on GitHub directly. Track all drafted comments and closure reasons in structured documents under `.gemini/github-issues/`.
4. **Separate Document Per Actionable Bug / Enhancement**: Hold a separate markdown document for each issue in Category 4 (`.gemini/github-issues/issues/issue-<NUMBER>.md`) containing root cause analysis, drafted GitHub response, reproduction steps, fix details, and validation status.
5. **Repro, Fix, & Emulator Validation**: For issues requiring a code fix, do a deep dive into the root cause, reproduce on emulator/tests, implement the fix, and validate no regressions.

## Progress
- [x] Step 1: Fetch all open GitHub issues (95), comments, and git history (`17d791d7-baed-47d5-b055-781c0d49b4c1`).
- [x] Step 2: Analyze all open issues and cross-reference with git commit history & codebase.
- [x] Step 3: Generate Master Triage & Drafted Comments Document + 26 per-issue deep-dive documents.
- [~] Step 4: Code fixes implemented in pass 1 (2026-10-26); pass-2 review (2026-10-27) found defects — corrections in progress.

## Pass 2 review (2026-10-27) — resume pointer
- Findings: `~/.gemini/jetski/brain/3400f02a-d388-483b-8f02-3bacbcb83923/review_pass2_findings.md`
- Worker ledger: `~/.gemini/jetski/brain/3400f02a-d388-483b-8f02-3bacbcb83923/flash-subagents.md`
- Key corrections: #635 real KSP JSON format + restore `discover_top_level_nodes` fallback; #661/#121 doc accuracy;
  #1814 clear-order bug; #1169 docId threaded via VariableState; #2261 conservative tween; #2456 no double modifier,
  empty Modifier ignored; #2181/#1798 pass-1 changes reverted (no-op / dead code); #573 renamed `setBrushWithContext`;
  #1117 dp units; #1138 compile regression test.
- Remaining: full verification run (validation Roborazzi + all-module compile), doc status updates, emulator run (needs user OK).

# Closed Testing Progress Log (Android v1.0)

Running record of what changed in each Play Console closed-testing release, kept so the
14-day-mandatory-testing final submission report (the one justifying to Google reviewers why the
app should be approved for production) can cite concrete, dated evidence instead of being
reconstructed from memory. Update this file at every release bump — don't batch it at the end.

## Status snapshot (as of 2026-09-09)

- **Track:** Closed testing (Alpha), 177 countries/regions.
- **Live release:** `7 (1.0.0)` — versionCode 7, approved and available to selected testers since
  2026-09-09 10:13.
- **Testers:** 25/25 opted in (third-party tester panel, "Private Testing Pro" plan). Mandatory
  period tracker shows Day 2 of 16.
- **Reports:** 0/3 ready.
- **Next upload:** `8 (1.0.0)` — versionCode 8, AAB built locally with the R8 keep-rule cleanup
  (Phase 2–8, see below) and `FREE_LAUNCH_MODE=true`. Built but **not yet uploaded** to Play
  Console as of this entry.

## Release history and what each build actually changed

### versionCode 4 — released 2026-09-04 20:18
Commits: `66a3aa3`, `9aba2a1`, `751b810`, `e696b13` (telemetry Phases 1–4), plus
`f8f619e` (offline Gemma model download UX + cellular consent), `f30ad2b` (temporarily disabled
`FLAG_SECURE` so testers can screenshot), `c542f71` (registered Firebase client, unblocked API
key), `3fd5a69`/`3ac4a88` (telemetry log sanitization + anonymous install ID), `c28ca3f` (in-app
diagnostic report generator / issue reporting), `30fe2d5` (dashboard/chart UI hardening),
`8248dfc` (background-worker test crash to verify the offline/release Crashlytics pipelines end to
end), `ee9c6fe` (non-fatal Crashlytics telemetry on unparseable PDFs), `c4c5137` (wired
`SwiftCrashReporterDelegate` on iOS for crash-telemetry parity with Android), `5f17ae9`
(versionCode bump itself, "full Gemma on-demand asset pack").

**What this means in plain terms:** Firebase Crashlytics was added end-to-end (fatal + non-fatal,
Android and iOS), R8 deobfuscation mapping upload was wired in, and the pipeline was verified with
a real test crash on a Pixel 9 before shipping. This is the "added Crashlytics" milestone.

### versionCode 5 — released 2026-09-06 (superseded quickly by 6)
Commit: `34ade77` — bumped `targetSdk` to 36 for current Google Play API-level requirements.

### versionCode 6 — released 2026-09-06 16:07
Commits: `22ddd53` (fixed a `fast-uri` dependency vulnerability flagged by CI in `web-prototype`,
unrelated to the shipped app binary but needed to keep CI green), `5b458f2` (moved the salary
countdown ribbon below the stats grid, theme styling pass), `65fcf42` (fixed password cursor
jumping to the wrong position on iOS during PDF unlock), `da2b1a6` (fixed a stale "success" modal
persisting across import dialog re-launches), `9c2ac33` (reworked the payslip import flow to be
file-first and self-explanatory).

**What this means in plain terms:** this is the "removed bugs" milestone — import-flow UX bugs and
a stale-state bug were fixed, plus a security-scan fix to keep the pipeline unblocked.

### versionCode 7 — released 2026-09-09 10:13 (current live release)
Commit: `542eb0c` — "restore v1.0 free-launch strategy and fix launch-mode test coupling."

**What this means in plain terms:** this is the "Fixed App Completeness" milestone. The paywall
gate is bypassed for v1.0 via `LaunchFlags.FREE_LAUNCH_MODE` so every feature (DSOP, Tax Planner,
Anomaly Detection) is fully interactive with no paywall-induced dead ends or error popups — this
directly addresses the "incomplete app" rejection class from Apple/Google review guidelines. See
[05_launch_strategy_and_resolution.md](05_launch_strategy_and_resolution.md) Section 0 for the full
rationale and the iOS resubmission this same flag unblocked.

### versionCode 8 — built 2026-09-07, **not yet uploaded** to Play Console
Commits `7497732` → `556fa9a` (R8 Phases 2–8) plus `ef9c3c2` (resource shrinking + proguard rule
dedup + CI verification) and `eb38acd` (fixed a typo in the litert/Gemma keep rule that would have
broken the on-device model at runtime under R8), finishing with `48d2fe6` (the versionCode 8 bump
itself).

**What this means in plain terms:** removed six categories of overly broad ProGuard/R8 keep rules
(blanket Room, generic `Exception`, blanket Ktor package, blanket Compose UI platform package,
blanket `@Serializable`-unscoped Companion keep, Koin Module-implementer) that were shrinking the
release APK/AAB less than necessary, while adding a CI check so keep-rule regressions are caught
automatically instead of only at release time. One real regression (`eb38acd`, the litert package
typo) was caught and fixed during this hardening pass before it could ship — worth calling out
explicitly in the final report as evidence of the testing rigor, not just as a fixed bug.

**Outstanding action:** upload the `composeApp-release.aab` (versionCode 8) to the Closed Testing
- Alpha track in Play Console. This has not happened yet as of this log entry.

## What still needs to happen before the Day-14 final submission

1. Upload versionCode 8 AAB to Closed Testing.
2. Keep 12+ testers active through the full 14-day mandatory window (Play Console currently shows
   25/25 opted in via the third-party panel, but Google's own 14-day counter is what governs
   production eligibility — confirm which counter is authoritative before submission).
3. Once Day 14 completes, collect the 3 pending crash/ANR reports referenced above (currently 0/3
   ready) — the final submission report should either show these as clean or document what was
   fixed in response to them.
4. Draft the final "why this app should be published" report citing this file's dated release
   history as evidence of iterative fixing (crash reporting → bug fixes → completeness fix →
   binary hardening), rather than reconstructing the narrative from git log at the last minute.

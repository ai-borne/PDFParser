# Internal & Closed Testing Progress Log (Android v1.0)

Running record of what changed in each Play Console release — Internal testing and Closed testing
tracks alike — kept so the 14-day-mandatory-testing final submission report (the one justifying to
Google reviewers why the app should be approved for production) can cite concrete, dated evidence
instead of being reconstructed from memory. Update this file at every release bump — don't batch it
at the end. Releases typically land on Internal testing first (fast, small-panel verification) and
are promoted to Closed testing once verified; the 14-day mandatory-testing clock applies only to the
Closed testing track, so each release's status line below states which track it's actually on.

## Status snapshot (as of 2026-09-09, post-upload, symbolication-verified)

- **Track:** Closed testing (Alpha), 177 countries/regions.
- **Live release:** `8 (1.0.0)` — versionCode 8, published via full rollout on 2026-09-09 20:12
  (Play Console Submission ID 6, submitted 19:49, published 20:12). Release name in Console:
  "8 (1.0.0) - R8 keep-rule hardening".
- **Queued behind this, on a separate track:** `9 (1.0.0)` is live on **Internal testing** (not
  Closed testing) as of 2026-09-10 20:46 — see versionCode 9 entry below for what it contains and
  why it hasn't been promoted yet. `10 (1.0.0)` is in progress on top of it, not yet built.
- **Testers:** 25/25 opted in (third-party tester panel, "Private Testing Pro" plan). Mandatory
  period tracker showed Day 2 of 16 as of the v8 upload.
- **Reports:** 0/3 ready as of the v8 upload.
- **Pre-upload verification done:** before uploading, the exact `composeApp-release.aab` was
  validated with `bundletool` (manifest dump confirmed versionCode 8 / versionName 1.0.0 /
  targetSdk 36, structural `validate` passed clean) and jarsigner-verified against the release
  keystore. It was then smoke-tested end-to-end by sideloading the built APK splits (installer
  spoofed to `com.android.vending` to pass `AppIntegrityChecker`) on both the Android emulator and
  a physical Pixel 9 — Dashboard/History/Insights/Settings all navigated cleanly, Crashlytics
  initialized, `FREE_LAUNCH_MODE` confirmed live ("Everything Included", no paywall), and no
  `FATAL EXCEPTION`/`ClassNotFoundException`/`NoSuchMethodError` appeared in logcat on either
  device — direct evidence the R8 Phase 2–8 keep-rule removals didn't break Koin/Room/Compose at
  runtime. The Pixel 9 was returned to its real Play-delivered v7 install afterward (uninstalled
  the sideload; reinstalled from Play Store) so the tester enrollment on that device stayed clean.
- **Play Console's own bundle diff (v8 vs v7), confirmed post-upload:** DEX size 14.7 MB → 12.8 MB
  (‑13%), download size 22.6 MB → 22 MB, optimisation 46% → 53%, obfuscation 47% → 54%, shrinking
  46% → 53%, and "Resource shrinking optimised" newly present in the R8 config — concrete
  confirmation the keep-rule cleanup had the intended effect. Permission list (12 permissions)
  verified identical to v7 — zero permission-surface drift from this release.
- **Crashlytics symbolication verified on v8, 2026-09-09 22:20:** the Phase 7 Companion-object
  keep-rule rescoping (`847edb1`) was the one v8 change with a real risk of silently breaking
  crash-report readability without breaking the app itself, so it hadn't been checked as part of
  the pre-upload smoke test. Verified directly on the physical Pixel 9 (same device as the
  pre-upload smoke test, running the live Play-delivered `1.0.0 (8)` build): unlocked the hidden
  Developer Sandbox (Settings header tapped 7×) and fired the built-in
  "Background Thread Crash (IO/Default)" trigger (`TestCrash.android.kt`,
  `triggerBackgroundTestCrash()`, added in `8248dfc`) via ADB. The resulting Crashlytics issue
  shows a fully readable stack trace —
  `com.payslipmax.pdfparser.telemetry.TestCrash_androidKt$triggerBackgroundTestCrash$1.invokeSuspend
  (TestCrash.android.kt:13)` — real package, class, file, and line number, not obfuscated
  (`a.b.c`-style) garbage. **Conclusion: v8's R8 keep-rule changes do not break Crashlytics
  symbolication; crash reports on this build can be trusted as-is.**
- **One unrelated Crashlytics issue explained, not a real crash:** a single
  `RemoteServiceException$CrashedByAdbException` ("shell-induced crash") appeared on v8 at
  2026-09-09 18:53:28, ~1 hour before the Play Console submission (19:49). This is not a real user
  crash — it's the expected side effect of the pre-upload sideload/uninstall/reinstall smoke-test
  sequence documented above (installing the split APKs then uninstalling and reinstalling the
  Play-delivered version via ADB), which Android's `ActivityThread` reports as this exact exception
  when a bound background service is disrupted mid-command. Muted/closed in Crashlytics so it
  doesn't skew the 14-day crash-free metrics or the final submission report.

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

### versionCode 8 — built 2026-09-07, published to Closed Testing 2026-09-09 20:12
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

**Status:** uploaded and published. See the status snapshot above for pre-upload verification
detail and the Play Console bundle-diff numbers confirming the shrink actually worked.

### versionCode 9 — released to Internal testing 2026-09-10 20:46, awaiting promotion to Closed testing
Commits: `bd0f2c4` (History screen ledger toggle affordance), `c80768f` (Digital Replica edit button label).

**What this means in plain terms:** two discoverability fixes discovered post-v8 during device testing:
- **History ledger toggle (bd0f2c4):** The "Historical Ledger Table" header row's expand/collapse control was only tappable on the small chevron icon, not the full row like the year ribbons below it. Fixed by making the entire row clickable (matching the year-header pattern), replacing the text-glyph chevron with Material Icons, and adding `contentDescription` strings for accessibility. Covered by new regression test `HistoricalLedgerCardUiTest.kt` verifying the whole row toggles expansion.
- **Digital Replica edit affordance (c80768f):** The edit/cancel button in the Payslip Digital Replica header was a bare pencil icon with no label, making its purpose (correction mode) invisible to users scanning the screen for how to fix wrong values. Fixed by stacking an "Edit"/"Cancel" label directly below the pencil icon (matching the bottom-nav icon-above-label convention) and updating the subtitle to name the action: "Tap a code for details · Edit to fix a wrong value". Updated tests to target the visible label instead of the now-hidden content description.

Note: `9fa3347` (iOS Gemma model delivery switch to On-Demand Resources) landed on the same branch around this time but is iOS-only with no Android version-code correlate — it does not appear in this Android release log by design.

**Rationale for v9:** both fixes are pure UI/UX improvements (no logic changes, no risk to parsing/data) and directly address discoverability issues identified during closed-testing device review. Releasing them keeps the app feeling responsive to tester feedback and demonstrates iterative polish in the final submission report.

**Status:** Released to the **Internal testing** track only (Play Console: "Available to internal testers," 1 version code, released Sep 10, 2026, 8:46 PM). **Not yet promoted to Closed testing** — Closed testing's latest release remains versionCode 8 (see status snapshot above); the 14-day mandatory-testing clock referenced in that snapshot is still running against v8, not v9. Promote to Closed testing once internal verification of v9 completes.

### versionCode 10 (in progress, not yet built) — Insights hero + further UX polish
Commits so far:
- `0122253` — Insights screen hero, first pass: added "Insights" title + month-picker + subtext
  inside an elevated `Surface` "ribbon," matching the Dashboard/History/Settings header pattern
  (Insights was the only main-tab screen missing a hero).
- `9f580f2` — Insights hero, refinement pass, after reviewing `0122253` on-device: removed the
  elevated `Surface` wrapper so the header sits directly on the screen background like every other
  tab (no more visually distinct "ribbon"); reordered to title → subtext → month picker; changed
  the subtext copy to "Monthly details: Track it. Know it. Own it."; made the month-picker chip
  full-width with generous padding for a larger thumb-friendly tap target; recolored the chip to
  the same `primaryContainer` blue used by the Dashboard officer-info card instead of the Material3
  default purple, for theme consistency. Also split `MonthSelectorDropdown` into a smaller helper
  to stay under the 50-line composable limit.

**What this means in plain terms:** Insights now has the same title/subtext header every other
tab has, and the month picker reads as a first-class, thumb-friendly control themed to match the
rest of the app rather than a generic Material3 chip.

**Status:** In progress on the same branch. More changes are planned before this is built and pushed to Internal testing; the push itself is queued behind v9's promotion from Internal testing to Closed testing (avoiding two releases in flight on different tracks at once).

## What still needs to happen before the Day-14 final submission

1. ~~Upload versionCode 8 AAB to Closed Testing.~~ Done 2026-09-09.
2. Keep 12+ testers active through the full 14-day mandatory window (Play Console currently shows
   25/25 opted in via the third-party panel, but Google's own 14-day counter is what governs
   production eligibility — confirm which counter is authoritative before submission). Note that
   pushing versionCode 8 mid-window is expected/normal — the mandatory-testing clock is track-based,
   not tied to a single release version — but reconfirm this in Play Console's own "Testing" tab
   rather than assuming it.
3. Once Day 14 completes, collect the 3 pending crash/ANR reports referenced above (currently 0/3
   ready) — the final submission report should either show these as clean or document what was
   fixed in response to them.
4. ~~Do a real-crash Crashlytics symbolication check on this specific build.~~ Done 2026-09-09,
   22:20 — verified clean/readable. See status snapshot above.
5. Draft the final "why this app should be published" report citing this file's dated release
   history as evidence of iterative fixing (crash reporting → bug fixes → completeness fix →
   binary hardening), rather than reconstructing the narrative from git log at the last minute.

# R8 Keep Rules — Phase 9–11 Rollout Plan (Serialization + Crashlytics cleanup)

**Date**: 2026-09-10
**Branch**: `release/ios-1.0.0-v6` (commit directly — this is the current closed-testing baseline,
already at `versionCode 8` from the prior R8 Phase 1–8 pass; see `git log --oneline --grep="r8" -i`
on this branch for that history).
**Scope**: `composeApp/proguard-rules.pro` only. No `shared`/`composeApp` Kotlin source changes.
**Source analysis**: `docs/Plan/05_R8_Keep_Rules_Recommendations.md`, findings #1–#4 and #8.
**Addresses now**: #1–#4 (kotlinx.serialization redundant keep rules) and #8 (Firebase Crashlytics
redundant keep rule).
**Explicitly deferred** (not in this plan — higher regression risk, needs a dedicated
native/JNI-focused pass): #5 (Koin annotation scope), #6 (global native-methods keep), #7 (LiteRT
wildcard keep). Revisit after this phase ships clean.

## Why this baseline, not `main`

`release/ios-1.0.0-v6` is what real closed testers are running today. It already contains a shipped,
real R8 optimization pass (Phase 1–8, commits `ef9c3c2`…`556fa9a`, released at `versionCode 8` per
`48d2fe6`) that `main` does not have. Branching from `main` would silently drop that prior work from
the baseline. This plan continues the existing phase numbering (Phase 9 onward) and commit style
(`chore(r8): ... (Phase N)`) directly on this branch.

## Non-negotiables carried into this plan

Per `CLAUDE.md`: phases execute in order, one at a time, no skipping ahead; every phase ends with a
fully successful build; no automated test suite exercises R8-stripped behavior in this repo (minify
is off in debug/unit tests), so **manual release-build device verification is the actual gate for
every phase below**, not `./gradlew check`.

---

## Phase 9 — Baseline capture (no commit)

Purpose: establish a known-good "before" state to compare against, and a rollback point.

1. Confirm clean working tree on `release/ios-1.0.0-v6` (`git status`).
2. `./gradlew :composeApp:assembleRelease` — archive the resulting `mapping.txt` as the
   pre-change baseline.
3. Install the baseline release build on a device:
   - Parse one payslip end-to-end, confirm persistence/read-back.
   - Force one test crash, confirm it appears correctly symbolicated in the Firebase Crashlytics
     console.

**Exit criteria**: baseline release build succeeds; baseline crash appears correctly in Crashlytics.
**Phase Handoff**: no tech debt (no code change this phase); build confirmed green.

---

## Phase 10 — Remove redundant kotlinx.serialization keep rules

**Commit**: `chore(r8): remove redundant kotlinx.serialization keep rules (Phase 10)`

1. Edit `composeApp/proguard-rules.pro`, delete the 4 blocks under "Kotlinx Serialization":
   - `-keepclassmembers class * implements kotlinx.serialization.KSerializer { *** INSTANCE; }`
   - `-keepclassmembers class * { @kotlinx.serialization.SerialName <fields>; @kotlinx.serialization.Serializable <fields>; }`
   - `-keepclassmembers @kotlinx.serialization.Serializable class * { *** Companion; }`
   - `-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }`
   - Leave the `-keepattributes *Annotation*,ElementValuePairs` line above them untouched (it's not
     a keep rule for these classes, it's a general attribute retention needed by the library).
2. `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` — cheap correctness gate.
3. `./gradlew :composeApp:assembleRelease`.
4. Device verification (the real gate): install the release build, parse payslips from at least two
   different grammar eras (e.g. one legacy statement, one modern grid), confirm the parse completes,
   persists via Room, and re-reads correctly — no `SerializationException` / `ClassNotFoundException`.

**Exit criteria**: release build succeeds; both grammar-era parse+persist checks pass on-device.
**Phase Handoff**: tech debt = none (pure deletion); build green; device verification passed
(record which two grammar eras were tested).

---

## Phase 11 — Remove redundant Firebase Crashlytics keep rule

**Commit**: `chore(r8): remove redundant Firebase Crashlytics keep rule (Phase 11)`

1. Edit `composeApp/proguard-rules.pro`, delete:
   - `-keepclassmembers class com.google.firebase.crashlytics.** { *; }`
   - Leave `-dontwarn com.google.firebase.crashlytics.**` untouched — separate, unrelated to this
     removal.
2. `./gradlew :composeApp:assembleRelease`.
3. Device verification: install the release build, force a test crash, confirm it appears in the
   Firebase Crashlytics console within the usual latency, correctly symbolicated against the new
   `mapping.txt` (the Crashlytics Gradle plugin, applied in `composeApp/build.gradle.kts:16`,
   uploads this automatically on release build — confirm the upload actually happened, don't
   assume).

**Exit criteria**: forced test crash captured and correctly symbolicated on the new build.
**Phase Handoff**: tech debt = none; build green; crash-capture verification passed.

If either Phase 10 or Phase 11 verification fails, bisect by reverting only that phase's commit —
they are independent changes to independent rule blocks.

---

## Phase 12 — Version bump + release candidate

**Commit**: `chore(release): bump versionCode to 9 for R8 keep-rule release`

1. Bump `versionCode` 8 → 9 in `composeApp/build.gradle.kts:168` (mirrors the convention used for
   the prior R8 pass, `48d2fe6`).
2. Full release build: `./gradlew :composeApp:assembleRelease` (or `bundleRelease` if shipping via
   Play App Bundle).
3. Re-run both device checks from Phase 10 and Phase 11 against this exact build artifact (not the
   intermediate per-phase builds) — this is the artifact that actually ships.

**Exit criteria**: `versionCode 9` release build succeeds; both parse/persist and crash-capture
checks pass on this exact artifact.
**Phase Handoff**: tech debt = none; build green; both verifications repeated and passing on the
shipping artifact.

---

## Phase 13 — Staged rollout to closed testing (no repo commit)

1. Upload the `versionCode 9` build to the lowest-exposure test track first (internal testing, not
   the full closed-testing group).
2. Hold a monitoring window of 24–48h, watching the Crashlytics dashboard for:
   - Any new exception signature not present in the Phase 9 baseline.
   - Crash-free-users rate vs. the Phase 9 baseline.
3. Promote to the full closed-testing group only after that window is clean.

**Exit criteria**: no new crash signatures attributable to this change; crash-free rate stable or
better vs. baseline.

---

## Phase 14 — Documentation close-out

**Commit**: `docs: mark R8 rules #1-4 and #8 addressed (Phase 9-11)`

1. Update `docs/Plan/05_R8_Keep_Rules_Recommendations.md`: mark findings #1–#4 and #8 as **done**,
   dated, with a one-line note on how each was verified (device parse test / forced-crash test).
2. Leave #5, #6, #7 explicitly flagged as still pending/deferred, with a pointer to this doc's
   rationale for deferring them (native/JNI regression risk, needs its own dedicated pass).

**Exit criteria**: doc reflects actual shipped state; nothing marked done that wasn't verified
on-device.

---

## Rollback

Each phase is one isolated commit on `release/ios-1.0.0-v6`. A regression traced to Phase 10 or
Phase 11 can be reverted independently (`git revert <sha>`) without touching the other, since the
two rule removals are unrelated. A regression traced to the version bump alone (Phase 12) reverts
cleanly without touching the rule changes.

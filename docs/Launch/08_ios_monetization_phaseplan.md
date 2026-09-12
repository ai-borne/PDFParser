# iOS Monetization Rollout — Phase-Wise Execution Plan (v1.1.1 → paid)

**Date**: September 11, 2026
**Scope**: iOS only. Executes Section 3 of
[07_platform_monetization_rollout.md](07_platform_monetization_rollout.md) as an explicit,
gated, phase-by-phase plan per this repo's non-negotiable execution rules (CLAUDE.md §0–2).
Android is untouched by this plan (still blocked on BillDesk KYC per doc 07 §4).

**Starting state**: `v1.1.0` free, live in App Store production. `v1.1.1` (UI changes only, no
monetization) is currently in App Review. `FREE_LAUNCH_MODE` is a single shared `const val = true`
in `shared/.../subscription/LaunchFlags.kt`. `RevenueCatApiKey.ios.kt` returns a **sandbox test
key** (`test_QOma...`) — this is the exact cause of the earlier Guideline 2.1 rejection on
`v1.0.0(2)`: RevenueCat could not resolve a real product because there was no real Apple app/key
behind it.

**Non-negotiable outcome**: no phase after this one ships to App Review with an IAP that isn't
100% wired end-to-end. Per RevenueCat's own rejection postmortems, the near-universal cause of
IAP-related 2.1 rejections is submitting before a sandbox purchase has actually been completed —
not a code defect. Phase 9 exists specifically to make that impossible to skip.

Each phase below ends with a **Phase Summary** (tech debt incurred / how it was resolved / build
+ test status) before the next phase starts, per CLAUDE.md's Phase Handoff Protocol. No phase
starts until the previous one is fully green.

---

## Phase 0 — Baseline

**Goal**: a frozen, verifiable snapshot of current state before anything changes.

- Confirm `v1.1.1` App Review status; do not touch `LaunchFlags`, `RevenueCatApiKey.ios.kt`, or
  any billing file while it's in review (a mid-review binary change is a different build than what
  was submitted — don't invalidate the pending review).
- Record current values as the rollback reference:
  - `LaunchFlags.FREE_LAUNCH_MODE = true`
  - `RevenueCatApiKey.ios.kt` → `test_QOmayJNDtTWprZuKRLJcsQKOOjW`
  - App Store Connect: no subscription product exists yet
  - RevenueCat dashboard: confirm whether an Apple app entry already exists (unclear from repo
    state alone — check the dashboard directly)
- Pull real v1.0/v1.1 install counts from App Store Connect Analytics (needed for the
  grandfather-clause decision in Phase 6 — doc 07 §3 step 5 flagged this as "confirm with real
  numbers, don't assume").
- No code changes in this phase. Exit criteria: baseline documented, `v1.1.1` review outcome known.

**Phase Summary** (completed 2026-09-12): no tech debt — no code touched. Baseline recorded with
real data (via new fastlane/App Store Connect API tooling under `iosApp/fastlane/`, added this
phase instead of manual dashboard checks each time): `v1.1.1` was `WAITING_FOR_REVIEW` at baseline
(cleared to `READY_FOR_SALE` before Phase 1 started); RevenueCat has no Apple app entry yet (only
`PayslipMax (Play Store)` exists); 1 real install in the last 60 days. Build/tests unaffected, still
green from `v1.1.1`'s own CI run.

---

## Phase 1 — Split the flag (mechanism only, still defaults to free)

**Goal**: make iOS/Android independently controllable without changing observable behavior yet.

- Edit `LaunchFlags.kt`:
  ```kotlin
  object LaunchFlags {
      const val FREE_LAUNCH_MODE_IOS: Boolean = true
      const val FREE_LAUNCH_MODE_ANDROID: Boolean = true
  }
  ```
- Update every call site (`SubscriptionManager.hasAccess()`, `BillingProvider` wiring, any
  feature-gate check) to read the platform-specific constant via `expect`/`actual` or a
  platform-scoped accessor — grep for `FREE_LAUNCH_MODE` first (`SubscriptionManagerTest.kt`,
  `SubscriptionManagerBillingTest.kt` reference it and need updating alongside).
- **This phase changes nothing about app behavior** — both flags stay `true`. This is purely the
  SSOT-preserving refactor from doc 07 §2, done as its own reviewable, testable step rather than
  bundled into the flag flip itself.
- Tests: update/extend `SubscriptionManagerTest` to assert the two constants are read
  independently (e.g. a test double that flips one without the other and checks only the matching
  platform gate responds).

**Exit criteria**: `./gradlew :shared:testDebugUnitTest :composeApp:testDebugUnitTest` green,
`ktlintCheck` green, app behavior on both platforms unchanged (still fully free).

**Phase Summary** (completed 2026-09-12, commit `8059f04` on `release/ios-1.0.0-v6`): tech debt =
none — mechanical constant split plus a new `isFreeLaunchModePlatform()` expect/actual (mirroring
the existing `isDebugBuild()` pattern in the same package), all call sites migrated in the same
commit (verified via grep, no stale `LaunchFlags.FREE_LAUNCH_MODE` references left). Added a test
proving the two platform flags are read independently rather than coupled. `:shared:testDebugUnitTest`,
`:composeApp:testDebugUnitTest`, `ktlintCheck`, the tech-debt/file-size audit, and the iOS framework
link check are all green (verified manually and re-verified by the pre-commit hook on commit).
Behavior confirmed unchanged — both flags still `true`.

---

## Phase 2 — App Store Connect: create the subscription product

**Goal**: the real product exists in ASC, independent of any app code.

- Confirm the Paid Applications Agreement is active for the developer account.
- Create an auto-renewable subscription group + subscription (`payslipmax_yearly_premium`, ₹199/yr
  per doc 07 §3, or whatever pricing is finalized — confirm price tier with the user before
  creating, this is a business decision not a technical one).
- Fill every required metadata field: display name, description, review screenshot (a screenshot
  of the actual paywall screen, not a placeholder — Apple rejects on missing/mismatched review
  screenshots), localized pricing.
- Get the product to **"Ready to Submit"** status in ASC — RevenueCat's own checklist calls this
  out explicitly as a precondition; a product stuck in "Missing Metadata" will silently fail to
  resolve later even if the code is correct.
- No code changes this phase — pure App Store Connect console work.

**Exit criteria**: subscription product shows "Ready to Submit" (or better) in ASC.

---

## Phase 3 — RevenueCat dashboard: wire the Apple app

**Goal**: RevenueCat can resolve the real product, not the sandbox placeholder.

*(This is the phase where `claude-in-chrome` is useful for driving the RevenueCat dashboard UI —
offer to drive it live once you're at the dashboard and want a second pair of hands on the
click-path, per the screenshot workflow you referenced.)*

- In RevenueCat dashboard → Project Settings → Apps: confirm/create the Apple App Store app entry
  (check whether one already exists from `v1.0.0` sandbox testing — don't create a duplicate).
- Attach the App Store Connect API key (or shared secret) so RevenueCat can validate receipts
  server-side.
- Product Catalog: attach the real Apple product ID from Phase 2 to the `premium` entitlement and
  a `yearly` package/offering.
- Copy the real **production** RevenueCat API key (`appl_...`) — this replaces the `test_...` key,
  but **do not commit it yet** (Phase 4 is the code change; keep Phase 3 as dashboard-only so it's
  independently verifiable/reversible).

**Exit criteria**: RevenueCat dashboard shows the entitlement resolving to a real, non-sandbox
Apple product; API key retrieved and held for Phase 4.

---

## Phase 4 — Code: real API key + platform flag still `true`

**Goal**: ship the real key through the pipe, but keep the paywall dark until Phase 8.

- Replace `RevenueCatApiKey.ios.kt`'s return value with the real `appl_...` key. Per this repo's
  security rule, confirm this key is safe to commit (RevenueCat public SDK keys are designed to be
  client-embedded, unlike the ASC API key from Phase 3 which must never enter the repo) — verify
  against RevenueCat's own docs on which key is which before committing.
- `FREE_LAUNCH_MODE_IOS` **stays `true`** in this phase — the point is to prove the RevenueCat SDK
  initializes against the real backend without yet exposing any paywall to real users.
- Add/extend a unit test asserting `revenueCatApiKey()` no longer returns a string prefixed
  `test_` (cheap regression guard against ever re-shipping the sandbox key to production — this
  exact mistake was the root cause of the earlier rejection).

**Exit criteria**: iOS build initializes RevenueCat against the production project; app still
100% free-behaving; tests green.

---

## Phase 5 — Verify the existing paywall wires up to the real product

**Goal**: confirm the already-built paywall works against Phase 3's real offering — this is a
verification/gap-fill phase, not a build-from-scratch phase.

- The paywall already exists: `composeApp/.../ui/screens/PremiumFeaturesScreen.kt`, driven by
  `PremiumFeaturesCatalog.kt` (SSOT `FeatureGate` → display-metadata mapping) and wired to
  `viewModel.launchPurchaseFlow(onResult)` / `viewModel.restorePurchases(onResult)`
  (`SubscriptionAccess.kt`). Apple's restore-button requirement is already satisfied
  (`onRestoreClick` exists). **Do not rebuild this** — Phase 5 is about proving it, not
  re-implementing it.
- Verify `launchPurchaseFlow`/`restorePurchases` actually source price/product data from
  RevenueCat's `Offering` (the Phase 3 real product), not a hardcoded/mock value — grep their
  implementation in the ViewModel and `RevenueCatBillingManager` to confirm.
- Run existing `InsightCardGatingTest` / `GatedNavigationInvariantTest` and confirm they still pass
  once Phase 3/4's real product ID is in play — these tests currently exercise the gating logic
  against whatever fixture/fake `BillingManager` they use, so check whether they need a new case
  for the real product ID or remain valid as-is.
- All copy/styling already presumably go through `AppStrings.kt`/`Theme.kt` per CLAUDE.md — spot
  check `PremiumFeaturesScreen.kt` for any drift before this phase closes.

**Exit criteria**: existing paywall confirmed to render real product/price data from the Phase 3
offering, restore-purchases confirmed present and functional, still gated off from real users
(`FREE_LAUNCH_MODE_IOS = true`), full test suite green. No new UI code unless a genuine gap is
found (e.g. price not sourced from the real offering) — if so, fix only that gap, surgically.

---

## Phase 6 — Grandfather-clause: confirmed **no**

**Decision (confirmed 2026-09-11)**: existing free installs do **not** keep free access. Every
user hits the real paywall once `v1.2` ships, regardless of install date — no separate
grandfather-check code path is built. Rationale: v1.1.x's live window is small (per doc 07 §3 step
5's own framing), so the cost of a clean cutover is low and avoids permanently carrying
install-date-branching logic in the gating path.

- No new code in this phase — this supersedes the "decide with Phase 0 data" framing; the decision
  is made. Phase 0's install-count pull is still worth doing for its own sake (understanding
  conversion exposure), but it no longer gates a decision here.
- Nothing in `SubscriptionManager.hasAccess()` should branch on install date. If such a branch is
  ever proposed later, treat it as a deliberate reversal of this decision, not a silent addition.

**Exit criteria**: this section stands as the recorded decision; no implementation work required.

---

## Phase 7 — Sandbox purchase verification (TestFlight)

**Goal**: prove the entire pipe works end-to-end before it's anywhere near App Review — this is
the single step whose absence caused the original rejection, per doc 07 §3 step 4.

- Ship a TestFlight build with `FREE_LAUNCH_MODE_IOS` flippable via a debug-only override (not the
  production flag itself) so the paywall is reachable in TestFlight without affecting the
  still-free production binary.
- Using a **sandbox Apple ID** (not a real account), complete an actual purchase through the
  TestFlight build:
  - Product fetch succeeds (no `STORE_PROBLEM`/generic `SKError`)
  - Purchase completes without error
  - Entitlement unlocks the gated feature immediately
  - Restore Purchases works on a fresh install/reinstall
- Also test failure paths: cancel mid-purchase, network-off during purchase — the app must fail
  gracefully, not crash or hang (a hang here is a 2.1 rejection risk independent of IAP config).

**Exit criteria**: a documented, successful sandbox purchase + restore, screenshotted, with no
manual workaround needed. Do not proceed to Phase 8 without this artifact existing.

---

## Phase 8 — Flip the real flag + submit

**Goal**: the actual production monetization switch.

- Flip `FREE_LAUNCH_MODE_IOS = false` in `main`. `FREE_LAUNCH_MODE_ANDROID` remains `true`,
  untouched (doc 07 §1/§5) — this is the one line that changes in this phase.
- Full regression: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`,
  `iosX64Test iosSimulatorArm64Test`, ktlint, corpus regression — this is a release build, run the
  full gate, not the incremental pre-commit subset.
- Submit as `v1.2` through App Review. Attach the Phase 7 sandbox-purchase evidence in App Review
  notes if there's any ambiguity in the paywall's placement (reviewers occasionally re-request
  this).
- This is the **second-to-last phase overall** the user asked for ("payment testing" phase) — note
  Phase 7 is the *sandbox* payment test (pre-submission) and this phase is the *submission* itself;
  if App Review comes back with a rejection, treat it as looping back to Phase 7, not forward.

**Exit criteria**: `v1.2` in App Review with monetization live, full test suite green, sandbox
evidence attached.

---

## Phase 9 — Distribution

**Goal**: paid app live in production.

- On approval: release `v1.2` to production (staged rollout if desired, to limit blast radius of
  any missed edge case).
- Monitor Crashlytics + RevenueCat dashboard (transaction success rate, entitlement grant latency)
  for the first 24–48 hours — RevenueCat's own guidance notes up to 24h propagation delay for new
  products, so don't treat early sandbox-vs-production discrepancies as a bug before that window
  passes.
- Close out this doc with actual production purchase metrics once available; update doc 07's
  "Current state" section to reflect iOS as paid-live.

**Exit criteria**: `v1.2` live in production, first real transactions confirmed successful in
RevenueCat dashboard, no elevated crash rate attributable to the billing path.

---

## Rejection-avoidance checklist (carried through every phase, source: RevenueCat's App Store
rejection guidance)

- [ ] Never submit an IAP-bearing build without a completed sandbox purchase first (Phase 7 gates
      Phase 8 on this).
- [ ] ASC product status is "Ready to Submit" before any submission referencing it (Phase 2).
- [ ] Production RevenueCat key (`appl_...`), not the sandbox `test_...` key, ships in the
      submitted binary (Phase 4, with a regression test).
- [ ] Restore Purchases is present and functional on the paywall (Phase 5/7).
- [ ] Purchase-flow failure paths (cancel, offline) degrade gracefully, not crash/hang (Phase 7).
- [ ] Entitlement unlocks the gated feature synchronously on purchase success — no dead-end where
      payment succeeds but content stays locked (Phase 7) — this is RevenueCat's #3 listed rejection
      cause.

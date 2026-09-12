---
name: ios-monetization-phase
description: Advance exactly one phase of the iOS monetization rollout (docs/Launch/08_ios_monetization_phaseplan.md), then stop. Use when the user asks to continue/resume iOS monetization work, or invokes /ios-monetization-phase directly.
---

# iOS monetization: one phase per session

This repo's iOS monetization rollout is intentionally executed **one phase per Claude Code
session** to avoid long-running sessions burning context/tokens across an 8+ phase plan. This
skill is the contained entry point for that — it does not re-derive the whole plan from scratch,
and it does not run past a single phase.

## 1. Load state before touching anything

- Read `docs/Launch/08_ios_monetization_phaseplan.md` in full.
- Check memory for `ios-monetization-phaseplan` (project) and `ios_monetization_tooling`
  (reference) entries — they carry prior sessions' confirmed facts and decisions (pricing,
  grandfather-clause call, sequencing choices) so you don't re-ask the user things already
  answered.
- Determine the next phase to run: the last phase in the doc with a filled-in **Phase Summary**
  (not a `template`/placeholder one) is done; the phase immediately after it is next.

## 2. Re-verify live state — don't trust memory as current

Memory and the doc are point-in-time snapshots. Before starting the next phase, re-check whatever
of these is relevant to it:

```bash
cd iosApp && set -a && source fastlane/.env && set +a && fastlane ios review_status
fastlane ios testflight_builds
fastlane ios install_counts days:30
```

(See `iosApp/fastlane/Fastfile` and `.env.example` for what's wired up — App Manager + Finance
App Store Connect API keys, both stored outside the repo at `~/.appstoreconnect/private_keys/`.)
For anything not scriptable (RevenueCat dashboard state, ASC agreement/tax-form status), ask the
user to check and paste a screenshot rather than assuming the last-known state still holds.

## 3. Execute only the next phase

- Follow that phase's steps and exit criteria from the doc exactly. Do not start the phase after
  it, even if it looks trivial or the momentum is there — that defeats the point of this skill.
- If the phase involves a business decision the doc calls out explicitly (pricing, grandfather
  clause, key roles, sequencing) and it isn't already answered in memory, ask via AskUserQuestion
  rather than assuming.
- Follow this repo's non-negotiable rules from `CLAUDE.md` throughout (TDD, 300-line limit, SSOT,
  surgical changes, run the actual exit-criteria commands rather than assuming green).

## 4. Close out — Phase Handoff Protocol

Before ending the session:

1. Run the phase's exit-criteria commands for real; don't report green without having run them.
2. Write a Phase Summary (tech debt / resolution / build+test status) per CLAUDE.md's Phase
   Handoff Protocol, both in chat and by replacing that phase's `template`/placeholder text in
   `docs/Launch/08_ios_monetization_phaseplan.md` with the real result (see how Phase 0/1 did
   this — commit `6fc6690`).
3. Update `07_platform_monetization_rollout.md` §0 "Current state" if anything there is now stale
   (it's a living tracker, unlike the dated historical logs 05/06 which should never be rewritten).
4. Update the `ios-monetization-phaseplan` memory file with the new state, and commit all code/doc
   changes with a message naming the phase completed.
5. Explicitly tell the user the phase is done and stop — don't continue into the next phase's work
   even speculatively.

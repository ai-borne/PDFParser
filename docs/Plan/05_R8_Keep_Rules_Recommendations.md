# R8 Keep Rules — Recommendations (r8-analyzer, heuristic pass)

**Date**: 2026-09-09
**Scope**: `composeApp/proguard-rules.pro` (Android release build only)
**Analysis path used**: Path C (heuristic) — AGP is 8.13.2, below the thresholds for the
quantitative paths (AGP >= 9.3.0 for the standalone task, R8 >= 9.3.7-dev for the config
analyzer). No per-rule byte/class counts were generated; findings below are pattern-based
against known bundled-rule cases.

## Applicability to this KMP app

R8/ProGuard is a JVM/D8-only optimization pass — it has no bearing on iOS.

- **`composeApp` (Android release variant)** — where `proguard-rules.pro` is applied
  (`isMinifyEnabled = true` in `composeApp/build.gradle.kts:193-199`). All recommendations
  below apply here.
- **`shared/` (`commonMain` + `androidMain`)** — compiled to JVM bytecode and bundled into
  the same Android APK, so it's subject to the same keep rules once part of the `composeApp`
  release build. Most of the `@Serializable` models and Koin DI live here — this is where the
  serialization/Koin recommendations bite hardest.
  ✅ Confirmed applicable — 2026-09-09.
- **`iosApp` / `iosMain`** — **not applicable**. iOS ships via Kotlin/Native → an
  Xcode-linked framework, not JVM bytecode. No R8/D8 pass runs; ProGuard rules do not apply.

## AGP upgrade — deferred, do not action yet

The skill's config check flagged AGP 8.13.2 → 9.0 as a general recommendation (Path A/B
quantitative analysis requires it). **This is explicitly deferred** — do not upgrade yet.

**Why**: confirmed via web search (2026-09-09) that KMP is not yet safely compatible with
AGP 9:
- AGP 9.0+ drops compatibility between the KMP Gradle plugin and the `com.android.application`
  / `com.android.library` plugins used in the same module — multiplatform Android modules need
  migration to a new plugin setup.
- IntelliJ Android Plugin support caps at AGP 8.13; Android Studio only supports AGP 9.0 from
  Otter 3 Feature Drop 2025.2.3 onward.
- Hilt and KSP (used transitively by several Android libraries) do not yet support AGP 9.0 —
  no clean workaround short of "severe workarounds."
- Legacy Variant APIs (needed as a stopgap via `android.enableLegacyVariantApi=true`) are
  slated for removal entirely in AGP 10 (H2 2026), so this is a short runway, not a real fix.

**Action**: revisit in a future sprint once KMP plugin / Hilt / KSP catch up. Track via
[kotlinlang.org AGP 9 migration guide](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html).

## Keep rule findings (`composeApp/proguard-rules.pro`)

### 1. `-keepclassmembers class * implements kotlinx.serialization.KSerializer { *** INSTANCE; }`
- **Keeps**: All `KSerializer` companion instances across the codebase, unscoped.
- **Action**: **Remove**. kotlinx.serialization is on 1.7.3 (`gradle/libs.versions.toml:18`),
  well past the 1.6.0+ version that bundles its own consumer ProGuard rules.

### 2. `-keepclassmembers class * { @kotlinx.serialization.SerialName <fields>; @kotlinx.serialization.Serializable <fields>; }`
- **Keeps**: Every `@SerialName`/`@Serializable`-annotated field app-wide, unscoped.
- **Action**: **Remove** — covered by the library's bundled rules.

### 3. `-keepclassmembers @kotlinx.serialization.Serializable class * { *** Companion; }`
- **Keeps**: Companion objects of all serializable classes, unscoped.
- **Action**: **Remove** — overlaps with #1/#2 and the library's bundled rules.

### 4. `-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }`
- **Keeps**: Any class exposing a `serializer()` method, unscoped.
- **Action**: **Remove** — library-bundled, redundant with the serialization block above.

### 5. `-keepclassmembers class * { @org.koin.core.annotation.* <fields>; @org.koin.core.annotation.* <methods>; }`
- **Keeps**: All Koin-annotated fields/methods app-wide, not scoped to this project's packages.
- **Action**: **Refine** — scope to the actual DI module package (e.g. `com.payslipmax.**`)
  instead of global `class *`.

### 6. `-keepclasseswithmembernames class * { native <methods>; }`
- **Keeps**: Every class in the app *and all dependencies* declaring a native method —
  fully unscoped, broadest rule in the file per the impact hierarchy.
- **Action**: **Refine** — narrow to the package(s) that actually need it
  (`com.google.ai.edge.litertlm.**`, already covered by rule #7 below). As written this is an
  unbounded safety-net rule.

### 7. `-keep class com.google.ai.edge.litertlm.** { *; }`
- **Keeps**: 100% of the LiteRT/Gemma package — package-wide wildcard, classes + members.
- **Action**: **Refine** — if this exists only for JNI native-method resolution, narrow to
  `-keepclasseswithmembernames class com.google.ai.edge.litertlm.** { native <methods>; }`
  instead of `{ *; }`.

### 8. `-keepclassmembers class com.google.firebase.crashlytics.** { *; }`
- **Keeps**: All members of every class under the Crashlytics package, package-wide wildcard.
- **Action**: **Remove**. Firebase Crashlytics has bundled its own consumer ProGuard rules
  since 17.x+.

## Subsumed rules

- Rule #1 and rule #4 are both subsumed by kotlinx.serialization's own bundled consumer rules
  (present since 1.6.0+; this project is on 1.7.3). **Action**: Remove both.

## Clean / no findings

No global disable rules (`-dontshrink`/`-dontoptimize`/`-dontobfuscate`), no inversion-operator
(`!`) rules, and no Gson/Retrofit/Room/Parcelable-related rules were found — these categories
are clean.

## Out of scope for this pass

- `-dontwarn` suppressions (PdfBox, Play Core, SLF4J, Ktor, Room paging, Compose UI platform) —
  not keep rules, not evaluated by this skill. Worth a manual look if chasing APK size further.
- No quantitative per-rule impact (class/method/field counts, optimization/shrinking/obfuscation
  %) — requires the AGP 9.3.0+ or R8 9.3.7-dev+ path, both blocked by the AGP-9 KMP
  incompatibility above.

## Next steps

1. Remove rules #1–#4 and #8 (library-bundled, safe to delete).
2. Refine rules #5, #6, #7 to narrower package/member scopes.
3. Re-run `./gradlew :composeApp:testDebugUnitTest` and a release build after each change to
   confirm nothing breaks (Crashlytics reporting, Koin DI resolution, LiteRT/Gemma native calls
   are the risk areas).
4. Re-run this analysis in quantitative mode once AGP 9 is safe to adopt for KMP.


---

Overall: mechanically easy (single-file edit, no source-code changes), but the risk varies a lot per rule. Here's the breakdown based on actual usage in this codebase:

Effort: trivial for all 8. Every change is a one-line delete or narrowing inside composeApp/proguard-rules.pro. No shared/ or composeApp/ Kotlin source changes needed. The real cost isn't writing the diff — it's verifying nothing breaks in the release build specifically, since ProGuard/R8 bugs never show up in debug builds or unit tests (minification is off in debug).

Rule	Change	Regression risk	Why
#1–#4 (serialization)	Remove	Low	kotlinx.serialization 1.7.3 bundles its own consumer rules; this is the most standard, well-tested "redundant rule" case in the ecosystem.
#8 (Crashlytics)	Remove	Low	34 files call the Crashlytics public API (FirebaseCrashlytics.getInstance()...), not internal SDK classes reflectively — the rule was protecting SDK internals the app doesn't touch directly, and Crashlytics AAR ships its own rules since 17.x.
#5 (Koin annotations)	Refine (scope narrower)	Low-Medium	Only 7 files reference org.koin.core.annotation at all, and the modules are mostly DSL-style (module { } in Koin.kt/AppModule.kt), not annotation-processed. Scoping to your package should be safe, but worth grepping those 7 files first to confirm which annotations are in play before narrowing.
#6 (global native-methods)	Refine	Medium	It's unscoped today, so narrowing it could stop protecting a native method in some other dependency you haven't inventoried (e.g. a transitive PDF/crypto lib). Low probability but non-zero — this is the one where "unknown unknowns" matter most.
#7 (LiteRT/Gemma)	Refine ({ *; } → native-methods-only)	Medium-High	68 references to LiteRT/Gemma across the codebase — this is your on-device inference engine, and JNI bridges are exactly where R8 misconfiguration causes silent UnsatisfiedLinkError/NoSuchMethodError crashes that only surface at runtime on a release build, not in tests. This is the one rule I'd stage separately and test hardest.
Practical rollout to keep risk low:

Do #1–#4 and #8 together first (same low-risk category, easy to bisect if something breaks).
Build release, install on a device, exercise Crashlytics (force a test crash) and basic parsing flow.
Do #5 next, alone — re-test Koin-dependent DI paths (app launch, screen navigation).
Do #6 and #7 last, together, on a separate build, since they're both native-method-related and most likely to interact — re-test the full Gemma/LiteRT fallback path specifically (tier 6 of the parsing pipeline), since that's tested least often and fails silently per the doc's SWOT notes on Gemma.
Standard mitigation either way: keep a mapping.txt from the release build so you can deobfuscate any crash that does slip through.
No test-suite coverage exists for R8-stripped behavior (per your own project rules, the corpus regression suite runs on JVM without minification) — so "tests pass" won't catch a bad keep-rule change. This needs a manual release-build device pass per the project's verify skill before it's considered done.
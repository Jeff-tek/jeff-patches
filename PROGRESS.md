# Jeff-tek Patches — Progress / Checkpoint

Crash-safe record. Update after every significant change. Resume from here if interrupted.

## Goal

Custom Morphe-compatible Android patches (ad removal / UX tweaks for apps Jeff uses),
built and released automatically via GitHub Actions.

## Status: ROOT CAUSE FOUND — all 3 fingerprints missed on device; rewritten against real smali

- v1.0.1-dev.2 device test: features NOT unlocked, ads NOT removed → "the patch didnt work at all".
- Root cause is **NOT** obfuscation drift — it is three concrete fingerprint bugs (see below).
- Fix committed to `dev` (v1.0.2-dev.1 expected): PremiumGate definingClass fixed, ads hooks retargeted
  to the real ad gates (`Lk4;->m()Z` + `Lra;->b`), every fingerprint verified against the v2.3.5.1 smali
  on disk, and misses now throw `PatchException` (loud) instead of silently no-oping.

## ROOT CAUSE (why "nothing worked")

1. **`PremiumGateFingerprint` could NEVER match.** `definingClass = "/hl3;"` — per patcher
   `StringComparisonType`, a declaration ending in `;` is compared with **ENDS_WITH**:
   `"Lhl3;".endsWith("/hl3;")` is always **false**. Unlock-all silently no-opped.
   Fix: `definingClass = "Lhl3;"` (EQUALS, version-pinned).
2. **`AdsEnabledFingerprint` targeted the wrong method.** `v32.c()` (`qaU9l5Yt` pref) is used only
   for **analytics event naming** ("Splash_NewUser/OldUser", "N_Cutter_Flow_2") — its two call sites
   (`SplashActivity`, `hs.P/Q`) never display an ad. Even when the patch matched, zero ads were removed.
   Deleted. The real master gate is `Lk4;->m()Z`.
3. **Real ad gates were never covered.** `Lk4;->m()Z` = "ads disabled" gate (pref `kmgJSgyY`), checked
   at **18 call sites**, every one treating `true` as BLOCK (ga lifecycle app-open interstitial,
   SplashActivity, AdActivity display, CategoryDetailActivity, q61 banner wrapper, ...).
   `Lra;->b(Context;Loa;)V` = App Open ad display entry (`AppOpenAd` wrapper), invoked from
   `ga.onActivityStopped` + delayed `Lh` runnable. Neither was patched.

## New hook set (all verified against disasm v2.3.5.1)

| Hook | Smali | Injection |
|---|---|---|
| Premium gate | `Lhl3;->e()Z` (field `a:Lzy0;` IGET_OBJECT anchor) | `return 1` |
| Master ads gate | `Lk4;->m()Z` (static, string `kmgJSgyY`) | `return 1` |
| App-open ad show | `Lra;->b(Landroid/content/Context;Loa;)V` | `return-void` |
| Banner no-ads flag | `BaseBannerAdActivity` ViewGroup-field methods (a0/c0/onResume/onDestroy/onStop) | `D=1` |

Patches now throw `PatchException` on fingerprint miss (collected across all hooks, thrown at end) so
Morphe Manager shows a clear failure instead of silently shipping an unpatched APK.

## Repo

- URL: https://github.com/Jeff-tek/jeff-patches
- Branches: `main` (stable), `dev` (pre-releases — ALL dev work happens here)
- Local clone: `/root/jeff-patches` (dev checked out)
- Add to Morphe Manager: https://morphe.software/add-source?github=Jeff-tek/jeff-patches

## Done

- [x] Created repo from `MorpheApp/morphe-patches-template` (via clone+push; template-create
      blocked by fine-grained PAT — clone & push achieves same result incl. both branches)
- [x] Actions permissions: `default_workflow_permissions=write`, can approve PRs (gh api PUT)
- [x] Pushed `main` + `dev` branches
- [x] Package rename `app.template` → `app.jefftek` (all .kt/.java/.kts, incl. smali
      `EXTENSION_CLASS` string in ExamplePatch.kt)
- [x] `patches/build.gradle.kts`: group + about block → "Jeff-tek Patches"
- [x] `settings.gradle.kts`: rootProject.name = "jeff-patches"
- [x] `extensions/extension/build.gradle.kts`: namespace = "app.jefftek.extension"
- [x] README.md: title/about/add-source URL/License customized
- [x] Issue templates: links → Jeff-tek/jeff-patches, dropped dead CONTRIBUTING.md links

## Commands (gotchas)

- Release pipeline: `feat:`/`fix:` on `dev` → auto pre-release; `chore:` → no release;
  merge `dev`→`main` (merge, NOT squash) → stable release. Never force-push release commits.
- NEVER hand-edit generated files: `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`,
  the `<!-- PATCHES_START/END -->` section of README.md. release.yml regenerates them.
- release.yml uses built-in `GITHUB_TOKEN` (no PAT secret needed for release; write
  permissions enabled via API). Gradle resolves the `app.morphe.patches` plugin from
  MorpheApp's GitHub Packages cross-org with GITHUB_TOKEN — worked out of the box.
- Actions PR-creation permission = `PUT /repos/{owner}/{repo}/actions/permissions/workflow`
  with `{"default_workflow_permissions":"write","can_approve_pull_request_reviews":true}`
  (the plain `/actions/permissions` endpoint silently ignores those fields).
- `Open a PR to main` workflow must run with ref=`dev` (workflow_dispatch) — dispatching
  on `main` makes SOURCE==TARGET → "No commits between" → skips. It auto-runs correctly
  on every dev push.

## Patch authoring cheat sheet (from morphe-patcher docs, 2026-08)

Workflow per app: get APK → disassemble (dex) → find hook method → write Fingerprint → write patch → push `feat:` → CI builds → test on device.

### Fingerprints (patches/src/main/kotlin/app/jefftek/patches/<app>/Fingerprints.kt)
- A fingerprint = partial method description; matches ONLY if ALL declared info matches.
- Declare as `object X : Fingerprint(...)` so failed matches include the name in stack traces.
- Fields: `definingClass` (implicit string comparison: full `Lcom/x/Y;`, package `:com/x/`, or class `/Y;`), `name`, `accessFlags`, `returnType`, `parameters` (obfuscated types → bare `L`), `filters`.
- Filters (order matters, spacing between filters allowed): `fieldAccess(opcode=IGET, definingClass="this", type="Ljava/util/Map;")`, `string("literal")`, `methodCall(...)`, `opcode(Opcode.X[, MatchAfterImmediately() | MatchFirst() | MatchAfterWithin(n)])`, `literal(1337)`, `anyInstruction(a, b)`, `OpcodesFilter.opcodesToFilters(...)`.
- `strings = listOf(...)` = unordered match (only for enum-init style methods).
- `classFingerprint = Fingerprint(...)` — find the class first, then the method within it.
- Robustness: fingerprint what WON'T change between updates (return type, params, literal strings, opcode patterns). NEVER obfuscated names. Prefer `methodCall(smali="Landroid/net/Uri;->parse(...)")` copy-paste style for clarity.
- Properties: `originalMethod` (read-only), `method` (mutable — accessed lazily, replaces original). `matchOrNull` variants avoid exceptions. `clearMatch()`+`match()` to refresh indexes after modifying a method. `matchAllOrNull()` to patch every match.
- Modify methods LAST-index → FIRST-index so earlier indexes stay valid.

### Patches (patches/src/main/kotlin/app/jefftek/patches/<app>/<Name>Patch.kt)
- `bytecodePatch(name="...", description="...", default=true) { compatibleWith(COMPATIBILITY_X); dependsOn(...); extendWith("x.mpe"); execute { ... }; finalize { ... } }`
- `resourcePatch` for resources (apktool-style), finalization runs in reverse execution order.
- Options: `val x by stringOption(name="...")` / `option<String>(...)` — configurable per-user.
- Extensions = precompiled DEX (.mpe) merged into app BEFORE patch executes — for complex logic, keep patches minimal. `extendWithAll { list }` for dynamic sets. Java class → smali ref like `Lapp/jefftek/extension/ExamplePatch;`.
- No `compatibleWith` → universal patch. `PatchException` = deliberate failure. Compatibility targets: `AppTarget(version=null, isExperimental=true)` for experimental.
- Naming: patch = what it does ("Remove ads"), description third-person present ending with period. Keep patches minimal.

### After authoring
- Commit `feat: Add <X> patch for <app>` → push `dev` → pre-release builds in ~2-3 min → `gh run list` to watch → patches-list.json auto-updates → test in Morphe Manager (enable pre-release source).
- If fingerprint doesn't match → patch fails loudly in CLI/Manager logs with fingerprint name. Iterate on the fingerprint.

## Local RE toolchain (ready)

- apktool 3.0.3 at `/root/tools/apktool_3.0.3.jar` (Java 21 present) — verified running.
- Disassemble target APK: `java -jar /root/tools/apktool_3.0.3.jar d app.apk -o out/` → smali in `out/smali*/`, resources in `out/res/`.
- Also useful: `aapt2 dump badging app.apk` for package/version, and Morphe CLI on a PC for local patch-testing (optional; Jeff's loop is push→CI).
- Get APKs from APKMirror or pull from device via ADB (if available).

## Next steps (resume here)

1. ✅ v1.0.1-dev.2 tested — features NOT unlocked, ads NOT removed → full root-cause found (see above):
   `/hl3;` ENDS_WITH bug + v32.c() is analytics-only + real gates (k4.m / ra.b) never covered.
2. ✅ Rewrote fingerprints + both patches (see "New hook set"), all verified against v2.3.5.1 disasm.
   Misses now throw PatchException (loud) instead of silent no-op.
3. ⏳ Pushed `fix:` to dev → CI builds v1.0.2-dev.1 → user re-tests in Morphe Manager (pre-releases ON).
   - Success = features unlocked AND no app-open/splash/interstitial/banner ads.
   - Patch fails loudly with fingerprint name = tell me the exact error → I re-analyze.
4. If v1.0.2-dev.1 passes → merge `dev`→`main` for stable release.

## Active app: MP3 Cutter and Ringtone Maker (ringtone.maker.mp3.cutter.audio)

- Bundle: `/data/data/com.termux/files/home/MorpheApp2Patch/*.apkm` → extracted/ + disasm/ (apktool 3.0.3).
- v2.3.5.1 (vc 235100), APKM type, icon color 0xFF0088 (magenta). InShot app; package obfuscated: real classes `com.inshot.videotomp3*`, helpers `hl3`, `v32`, `k4`, `ra`, `zy0` in `smali_classes3/`.
- Ad SDKs: AdMob (banners + App Open) + InMobi. AdMob unit: `ca-app-pub-5434446882525782/4817097772`.
- Pref keys: `qaU9l5Yt` (new-user analytics, NOT ads — old analysis was wrong), `kmgJSgyY` ("no ads" flag,
  default false; drives `Lk4;->m()Z` + `BaseBannerAdActivity.D`).

### Real ad architecture (v2.3.5.1)
1. **App Open / interstitial**: `Lga` (activity lifecycle callbacks, registered in `v32.b()`) →
   `onActivityStarted` launches `AdActivity` when `Lra;->a()` true ∧ `Lk4;->m()` false ∧ `Li91;->c()` true
   ∧ `Li91;->k()` false; `onActivityStopped` shows via `Lra;->b()` (or delayed `Lh` runnable). `Loa` =
   ad config (unit id, `Lra` instance, AdActivity class).
2. **Splash**: `SplashActivity` gates splash ad on `Lk4;->m()`.
3. **Banner**: `BaseBannerAdActivity` (11 subclasses) — `D:Z` no-ads flag hides container (`c0`→GONE)
   and skips `Lkn` AdView creation in `onResume`; `Lkn;->b(String)` creates/loads the AdMob AdView.

### Patches authored (v1.0.2-dev.1)
- `ringtonemaker/Fingerprints.kt`: PremiumGateFingerprint (Lhl3), AdsDisabledFingerprint (k4.m),
  AppOpenShowFingerprint (ra.b), BannerDisplayFingerprint (BaseBannerAdActivity).
- `ringtonemaker/UnlockAllFeaturesPatch.kt` — hl3.e() → return 1 (throws on miss).
- `ringtonemaker/RemoveAdsPatch.kt` — k4.m() → return 1, ra.b() → return-void, banner D=1
  (all hooks attempted, combined throw on miss).
- `shared/Constants.kt`: `COMPATIBILITY_RINGTONE_MAKER` (APKM, version "2.3.5.1").

## Notes

- Example patches deleted (was: kept as reference). All real patches now under `ringtonemaker/`.
- Jeff's loop: push → CI builds → use artifact. No local Gradle builds (Termux-unfriendly).

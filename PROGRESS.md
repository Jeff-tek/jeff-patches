# Jeff-tek Patches — Progress / Checkpoint

Crash-safe record. Update after every significant change. Resume from here if interrupted.

## Goal

Custom Morphe-compatible Android patches (ad removal / UX tweaks for apps Jeff uses),
built and released automatically via GitHub Actions.

## Status: SETUP COMPLETE, first CI release pending

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
  permissions enabled via API).
- Possible gotcha: Gradle resolves `app.morphe.patches` plugin from MorpheApp's GitHub
  Packages registry with GITHUB_TOKEN (cross-org). If CI fails with 401 on package
  resolution → need classic PAT with `read:packages` as repo secret, wired into
  `settings.gradle.kts` credentials (gpr.user / gpr.key gradle props or env).

## Current todos

- [ ] First CI release: push `feat:` commit → verify dev pre-release `.mpp` + patches-list.json
- [ ] Verify patches-list shows "Example Patch" (compatible with fake com.example.app)
- [ ] Pick first real target app + author first fingerprint/patch
- [ ] Test apply via Morphe Manager on device (add source, patch, install)

## Next steps (resume here)

1. Commit current changes with `feat:` message → push `dev` → watch Actions run
2. Check run: `gh run list` / `gh run watch`
3. Then decide first real app target with Jeff.

## Notes

- Example patches (ExamplePatch/Fingerprints/InternalPatch, fake "com.example.app") kept as
  reference — will be replaced with real patches.
- Jeff's loop: push → CI builds → use artifact. No local Gradle builds (Termux-unfriendly).

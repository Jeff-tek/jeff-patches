package app.jefftek.patches.ringtonemaker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.jefftek.patches.shared.Constants.COMPATIBILITY_RINGTONE_MAKER

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes app-open, splash, interstitial and banner ads from MP3 Cutter and Ringtone Maker.",
    default = true
) {
    compatibleWith(COMPATIBILITY_RINGTONE_MAKER)

    execute {
        // Every fingerprint gets a shot; misses are collected and reported loudly at the
        // end so a single drift never silently degrades the whole patch (or kills the
        // remaining hook attempts).
        val failures = mutableListOf<String>()

        // Master "ads disabled" gate: forced true blocks ads at every call site
        // (app-open interstitial, splash, category, AdActivity display, banner wrapper, ...).
        val adGateMatches = AdsDisabledFingerprint.matchAllOrNull()
        if (adGateMatches == null) {
            failures += "AdsDisabledFingerprint (Lk4;->m()Z) not found"
        } else {
            adGateMatches.forEach { match ->
                match.method.addInstructions(
                    0,
                    """
                        const/4 v0, 0x1
                        return v0
                    """
                )
            }
        }

        // Background App Open (interstitial) ad launcher: return immediately so the ad
        // is never displayed from the onActivityStopped / delayed-runnable paths.
        val appOpenMatches = AppOpenShowFingerprint.matchAllOrNull()
        if (appOpenMatches == null) {
            failures += "AppOpenShowFingerprint (Lra;->b) not found"
        } else {
            appOpenMatches.forEach { match ->
                match.method.addInstructions(0, "return-void")
            }
        }

        // Force the "no ads" flag in BaseBannerAdActivity: `onResume` then skips banner
        // creation and `a0` hides the banner container in every extending activity.
        val bannerMatches = BannerDisplayFingerprint.matchAllOrNull()
        if (bannerMatches == null) {
            failures += "BannerDisplayFingerprint not found"
        } else {
            bannerMatches.forEach { match ->
                match.method.addInstructions(
                    0,
                    """
                        const/4 v0, 0x1
                        iput-boolean v0, p0, Lcom/inshot/videotomp3/BaseBannerAdActivity;->D:Z
                    """
                )
            }
        }

        if (failures.isNotEmpty()) {
            throw PatchException("Remove ads — fingerprint(s) did not match: ${failures.joinToString("; ")}")
        }
    }
}

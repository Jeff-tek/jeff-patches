package app.jefftek.patches.ringtonemaker

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.jefftek.patches.shared.Constants.COMPATIBILITY_RINGTONE_MAKER

@Suppress("unused")
val unlockAllFeaturesPatch = bytecodePatch(
    name = "Unlock all features",
    description = "Unlocks all premium features in MP3 Cutter and Ringtone Maker.",
    default = true
) {
    compatibleWith(COMPATIBILITY_RINGTONE_MAKER)

    execute {
        // Fail loudly on a miss so it is obvious in the patcher log when the
        // fingerprint no longer matches, instead of silently shipping an unpatched APK.
        val matches = PremiumGateFingerprint.matchAllOrNull()
            ?: throw PatchException(
                "PremiumGateFingerprint (Lhl3;->e) did not match — cannot unlock features in this build."
            )
        matches.forEach { match ->
            match.method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """
            )
        }
    }
}

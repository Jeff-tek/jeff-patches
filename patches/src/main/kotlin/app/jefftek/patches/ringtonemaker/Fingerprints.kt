package app.jefftek.patches.ringtonemaker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Fingerprints for MP3 Cutter and Ringtone Maker (ringtone.maker.mp3.cutter.audio) v2.3.5.1.
 *
 * App is InShot's MP3 Cutter & Ringtone Maker. Code is partially obfuscated (hl3, v32),
 * so fingerprints pin to the exact version target.
 */

/**
 * `Lhl3;->e()Z` — the app-wide "is premium / feature unlocked" gate.
 *
 * Read at 8+ call sites (ContactsActivity, FinishActivity, PickerActivity, ringtone
 * category detail, etc.). Returning `false` triggers ad display and premium locks;
 * returning `true` unlocks features and skips premium-gated ads.
 *
 * Per patcher docs, do NOT fingerprint exact access flags (exact int comparison — any
 * R8-added flag bit rejects the match). Class/method names are obfuscated per build but
 * are the only identifiers available for this gate (no stable string literal), so keep
 * them as best-effort pins and patch via matchAllOrNull so a miss degrades to a no-op.
 */
object PremiumGateFingerprint : Fingerprint(
    definingClass = "/hl3;",
    name = "e",
    returnType = "Z",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Lzy0;"
        ),
    )
)

/**
 * The master "are ads enabled" switch (formerly `Lv32;->c()Z`, obfuscated per build).
 *
 * Reads the `qaU9l5Yt` SharedPreferences flag (default `true` = ads ON) and caches it in a
 * boolean field. Checked by SplashActivity (splash ads) and the `hs` ad scheduler
 * (interstitial/full-screen AdActivity). Returning `false` disables all scheduled ads.
 *
 * Per patcher docs, do NOT fingerprint obfuscated class/method names or exact access flags.
 * The pref key `qaU9l5Yt` is a stable literal (the app can't rename its own pref key), so
 * match purely on the boolean return type + that string anywhere in the method.
 */
object AdsEnabledFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("qaU9l5Yt"),
)

/**
 * Banner show/hide in `BaseBannerAdActivity`.
 *
 * All ~10 ad-bearing activities extend this class and call the banner method from `onCreate`.
 * When field `D` (the "no ads" pref `kmgJSgyY`) is `true`, the banner container is
 * hidden; when `false`, the banner stays visible. Forcing `D = true` hides the banner
 * in every activity.
 *
 * Per patcher docs, do NOT fingerprint obfuscated method names (`a0`) or exact access
 * flags. Match on the real (non-obfuscated) class + void return + any ViewGroup field
 * access (get or put — the smali uses `iget-object`, so an `IGET` pin would never match).
 */
object BannerDisplayFingerprint : Fingerprint(
    definingClass = "Lcom/inshot/videotomp3/BaseBannerAdActivity;",
    returnType = "V",
    filters = listOf(
        fieldAccess(
            definingClass = "this",
            type = "Landroid/view/ViewGroup;"
        ),
    ),
    // The injected code writes p0->D, so only instance methods are patchable.
    custom = { method, _ -> !AccessFlags.STATIC.isSet(method.accessFlags) }
)

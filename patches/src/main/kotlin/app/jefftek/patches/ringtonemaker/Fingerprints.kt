package app.jefftek.patches.ringtonemaker

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * Fingerprints for MP3 Cutter and Ringtone Maker (ringtone.maker.mp3.cutter.audio) v2.3.5.1.
 *
 * App is InShot's MP3 Cutter & Ringtone Maker. Code is partially obfuscated (hl3, v32, k4, ra),
 * so fingerprints pin to the exact version target (compatibleWith v2.3.5.1).
 *
 * NOTE on `definingClass`: per patcher StringComparisonType semantics, "/hl3;" is compared with
 * ENDS_WITH and can NEVER match "Lhl3;". Always declare the full type `L...;` (EQUALS) or a
 * suffix like "hl3;" (ENDS_WITH that actually holds).
 */

/**
 * `Lhl3;->e()Z` — the app-wide "is premium / feature unlocked" gate.
 *
 * Read at 8+ call sites (ContactsActivity, FinishActivity, PickerActivity, ringtone
 * category detail, etc.). Returns true when the user is premium.
 *
 * Match anchors (verified against v2.3.5.1 smali): class `Lhl3;`, method `e`, boolean return,
 * and the `iget-object v0, p0, Lhl3;->a:Lzy0;` field read (field `a` of `this` is `Lzy0;`).
 * The `Lzy0;` field access is the stable anchor — `zy0` is the purchase-state holder.
 */
object PremiumGateFingerprint : Fingerprint(
    definingClass = "Lhl3;",
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
 * `Lk4;->m()Z` — the master "ads disabled" gate.
 *
 * Reads the `kmgJSgyY` SharedPreferences flag (default `false` = ads on) and returns true only
 * when the flag is set AND the ad config object exists. Checked at 18 call sites across the app
 * (ga activity-lifecycle app-open interstitial path, SplashActivity, AdActivity display,
 * CategoryDetailActivity, q61 banner wrapper, ...) — every call site treats `true` as
 * "block / skip the ad". Forcing `true` disables ads everywhere the app consults it.
 */
object AdsDisabledFingerprint : Fingerprint(
    definingClass = "Lk4;",
    name = "m",
    returnType = "Z",
    strings = listOf("kmgJSgyY"),
    custom = { method, _ -> AccessFlags.STATIC.isSet(method.accessFlags) }
)

/**
 * `Lra;->b(Landroid/content/Context;Loa;)V` — the App Open (interstitial) ad display entry.
 *
 * `Lra` wraps `Lcom/google/android/gms/ads/appopen/AppOpenAd`. `b(...)` loads unit config and
 * schedules/displays the app-open ad; it is invoked from `Lga;->onActivityStopped` (app goes to
 * background) and from a delayed `Lh` runnable. Returning immediately prevents the ad from
 * ever being shown on those paths.
 */
object AppOpenShowFingerprint : Fingerprint(
    definingClass = "Lra;",
    name = "b",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;", "L"),
)

/**
 * Banner show/hide in `BaseBannerAdActivity`.
 *
 * All ~10 ad-bearing activities extend this class and call the banner method from `onCreate`.
 * When field `D` (the "no ads" pref `kmgJSgyY`) is `true`, the banner container is hidden and
 * banner creation is skipped. Forcing `D = true` hides the banner in every activity.
 *
 * Matches (verified against v2.3.5.1 smali): every non-static void method in the class that
 * accesses a `ViewGroup`-typed field (`a0`, `c0`, `onResume`, `onDestroy`, `onStop`). All are
 * safe to prepend `D = true` to; `onResume` is the one that skips banner creation.
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

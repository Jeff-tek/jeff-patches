package app.jefftek.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_RINGTONE_MAKER = Compatibility(
        name = "MP3 Cutter and Ringtone Maker",
        packageName = "ringtone.maker.mp3.cutter.audio",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0xFF0088, // Magenta background of the app icon.
        targets = listOf(
            AppTarget(
                version = "2.3.5.1"
            )
        )
    )

}

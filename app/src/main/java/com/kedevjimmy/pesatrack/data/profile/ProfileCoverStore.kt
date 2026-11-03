package com.kedevjimmy.pesatrack.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/** An optional user-chosen cover, stored only in PesaTrack's private app files. */
internal class ProfileCoverStore(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "profile")
    private val coverFile = File(directory, "cover.jpg")

    fun load(): Bitmap? = runCatching {
        if (!coverFile.isFile || coverFile.length() > MAX_SAVED_BYTES) return null
        BitmapFactory.decodeFile(coverFile.absolutePath)
    }.getOrNull()

    /** A failed write must keep the previous cover intact. */
    fun save(bitmap: Bitmap): Boolean {
        val crop = profileCoverCropRect(bitmap.width, bitmap.height) ?: return false
        val pending = File(directory, "cover.pending")
        return try {
            if (!directory.isDirectory && !directory.mkdirs()) return false
            val cropped = Bitmap.createBitmap(bitmap, crop.left, crop.top, crop.width, crop.height)
            try {
                val scaled = Bitmap.createScaledBitmap(cropped, 1024, 440, true)
                try {
                    FileOutputStream(pending).use { stream ->
                        if (!scaled.compress(Bitmap.CompressFormat.JPEG, 86, stream)) {
                            throw IllegalStateException("Cover encoding failed")
                        }
                        stream.fd.sync()
                    }
                    pending.renameTo(coverFile)
                } finally {
                    if (scaled !== cropped) scaled.recycle()
                }
            } finally {
                if (cropped !== bitmap) cropped.recycle()
            }
        } catch (_: Exception) {
            false
        } finally {
            if (pending.exists()) pending.delete()
        }
    }

    fun clear(): Boolean = !coverFile.exists() || coverFile.delete()

    private companion object {
        const val MAX_SAVED_BYTES = 2 * 1024 * 1024L
    }
}

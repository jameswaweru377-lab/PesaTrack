package com.kedevjimmy.pesatrack.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/** A single local-only profile thumbnail, independent of any financial ledger or remote account. */
class ProfilePhotoStore(context: Context) {
    private val app = context.applicationContext
    private val directory = File(app.filesDir, "profile")
    private val photoFile = File(directory, "avatar.jpg")

    fun load(): Bitmap? = runCatching {
        if (!photoFile.isFile || photoFile.length() > MAX_SAVED_BYTES) return null
        BitmapFactory.decodeFile(photoFile.absolutePath)
    }.getOrNull()

    /** Replaces the saved photo only after the full compressed thumbnail is safely written. */
    fun save(bitmap: Bitmap): Boolean {
        if (bitmap.width <= 0 || bitmap.height <= 0) return false
        val temp = File(directory, "avatar.pending")
        return try {
            if (!directory.isDirectory && !directory.mkdirs()) return false
            val thumbnail = Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true)
            try {
                FileOutputStream(temp).use { stream ->
                    if (!thumbnail.compress(Bitmap.CompressFormat.JPEG, 86, stream)) {
                        throw IllegalStateException("Could not encode profile image")
                    }
                    stream.fd.sync()
                }
                temp.renameTo(photoFile)
            } finally {
                if (thumbnail !== bitmap) thumbnail.recycle()
            }
        } catch (_: Exception) {
            false
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun clear(): Boolean = !photoFile.exists() || photoFile.delete()

    /** Read bounded, sampled local media supplied by the system photo picker. */
    fun decodeSelected(uri: Uri): Bitmap? = runCatching {
        val bytes = ByteArrayOutputStream()
        app.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_SELECTED_BYTES) return null
                bytes.write(buffer, 0, count)
            }
        } ?: return null
        val encoded = bytes.toByteArray()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encoded, 0, encoded.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_DECODE_DIMENSION ||
            bounds.outHeight / sample > MAX_DECODE_DIMENSION
        ) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            encoded, 0, encoded.size,
            BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        ) ?: return null
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(encoded))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270f
            else -> 0f
        }
        val mirror = orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE
        if (degrees == 0f && !mirror) decoded else {
            val matrix = Matrix().apply {
                if (mirror) {
                    if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) postScale(1f, -1f)
                    else postScale(-1f, 1f)
                }
                postRotate(degrees)
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }
    }.getOrNull()

    private companion object {
        const val THUMBNAIL_SIZE = 320
        const val MAX_SAVED_BYTES = 1024 * 1024L
        const val MAX_SELECTED_BYTES = 12 * 1024 * 1024
        const val MAX_DECODE_DIMENSION = 1024
    }
}

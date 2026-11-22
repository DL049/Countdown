package com.example.countdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/** Custom widget background images: one image per event, stored in app-private storage. */
object WidgetBg {
    private const val SOURCE_MAX = 1024

    private fun file(c: Context, eventId: String) = File(c.filesDir, "bg_$eventId.jpg")

    fun exists(c: Context, eventId: String) = file(c, eventId).exists()

    fun delete(c: Context, eventId: String) {
        file(c, eventId).delete()
    }

    /** Copies the picked image into private storage, downscaled and EXIF-rotated. */
    fun saveSource(c: Context, eventId: String, uri: Uri): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > SOURCE_MAX * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = c.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        val rotation = c.contentResolver.openInputStream(uri)?.use { s ->
            when (ExifInterface(s).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (decoded == null) {
            false
        } else {
            val scale = min(1f, SOURCE_MAX.toFloat() / max(decoded.width, decoded.height))
            val m = Matrix().apply {
                postScale(scale, scale)
                postRotate(rotation)
            }
            val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
            FileOutputStream(file(c, eventId)).use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            true
        }
    } catch (e: Exception) {
        false
    }

    fun readBase64(c: Context, eventId: String): String? {
        val f = file(c, eventId)
        return if (f.exists()) Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else null
    }

    fun writeBase64(c: Context, eventId: String, data: String) {
        file(c, eventId).writeBytes(Base64.decode(data, Base64.DEFAULT))
    }

    /** Small bitmap for the preview in the edit screen. */
    fun thumb(c: Context, eventId: String): Bitmap? {
        val f = file(c, eventId)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 2 })
    }

    /**
     * Renders the widget background: the image centre-cropped to the widget's size,
     * rounded corners and an overlay baked in. Capped in size to stay within the
     * RemoteViews/binder memory limit.
     */
    fun render(
        c: Context, eventId: String, widthDp: Int, heightDp: Int,
        scrimPercent: Int, lightText: Boolean,
    ): Bitmap? {
        val f = file(c, eventId)
        if (!f.exists()) return null
        val src = BitmapFactory.decodeFile(f.path) ?: return null
        val d = c.resources.displayMetrics.density
        var w = widthDp * d
        var h = heightDp * d
        val k = min(1f, 480f / max(w, h))
        w *= k
        h *= k
        val out = Bitmap.createBitmap(max(1, w.toInt()), max(1, h.toInt()), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val s = max(out.width / src.width.toFloat(), out.height / src.height.toFloat())
        val m = Matrix().apply {
            setScale(s, s)
            postTranslate((out.width - src.width * s) / 2f, (out.height - src.height * s) / 2f)
        }
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(m)
        }
        val rect = RectF(0f, 0f, out.width.toFloat(), out.height.toFloat())
        val r = 24f * d * k
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader }
        canvas.drawRoundRect(rect, r, r, paint)
        // dark overlay behind white text, light overlay behind dark text
        val alpha = (scrimPercent.coerceIn(0, 80) * 255 / 100) shl 24
        val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = alpha or (if (lightText) 0x000000 else 0xFFFFFF) }
        canvas.drawRoundRect(rect, r, r, scrim)
        src.recycle()
        return out
    }
}

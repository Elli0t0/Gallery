package com.elli0t0.gallery.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import kotlin.math.pow
import kotlin.math.sqrt

data class Adjustments(
    val lightBalance: Float = 0f,
    val brightness: Float = 0f,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val saturation: Float = 0f,
    val tint: Float = 0f,
    val temperature: Float = 0f,
    val sharpness: Float = 0f,
    val vignette: Float = 0f
)

data class TransformState(
    val cropRatio: Float = 0f,
    val straighten: Float = 0f,
    val rotateSteps: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false
)

data class EditSnapshot(val transform: TransformState, val adjustments: Adjustments)

object EditClipboard {
    var snapshot: EditSnapshot? = null
}

val FILTERS: List<Pair<Int, Adjustments>> = listOf(
    R.string.filter_original to Adjustments(),
    R.string.filter_vivid to Adjustments(saturation = 30f, contrast = 15f),
    R.string.filter_warm to Adjustments(temperature = 35f, saturation = 10f),
    R.string.filter_cool to Adjustments(temperature = -35f),
    R.string.filter_mono to Adjustments(saturation = -100f),
    R.string.filter_fade to Adjustments(contrast = -20f, brightness = 10f, saturation = -20f)
)

fun decodeBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(input, null, bounds)
        input.close()
        var sample = 1
        while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2
        val input2 = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeStream(input2, null, options)
        input2.close()
        bitmap
    } catch (_: Exception) {
        null
    }
}

fun applyTransform(source: Bitmap, t: TransformState): Bitmap {
    var bitmap = source
    if (t.cropRatio > 0f) {
        val target = t.cropRatio
        val w = bitmap.width
        val h = bitmap.height
        val current = w.toFloat() / h
        val (nw, nh) = if (current > target) (h * target).toInt() to h else w to (w / target).toInt()
        val x = (w - nw) / 2
        val y = (h - nh) / 2
        bitmap = Bitmap.createBitmap(bitmap, x, y, nw, nh)
    }
    if (t.straighten != 0f) {
        val matrix = Matrix().apply { postRotate(t.straighten) }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    repeat(t.rotateSteps.mod(4)) {
        val matrix = Matrix().apply { postRotate(90f) }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    if (t.flipH) {
        val matrix = Matrix().apply {
            postScale(-1f, 1f)
            postTranslate(bitmap.width.toFloat(), 0f)
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    if (t.flipV) {
        val matrix = Matrix().apply {
            postScale(1f, -1f)
            postTranslate(0f, bitmap.height.toFloat())
        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    return bitmap
}

fun applyAdjustments(source: Bitmap, a: Adjustments): Bitmap {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)
    val expFactor = 2.0f.pow(a.exposure / 100f)
    val contrastFactor = 1f + a.contrast / 100f
    val gamma = (1f - a.lightBalance / 200f).coerceIn(0.2f, 3f)
    val satFactor = 1f + a.saturation / 100f
    val cx = w / 2f
    val cy = h / 2f
    val maxDist = sqrt(cx * cx + cy * cy)
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            val pixel = pixels[i]
            var r = Color.red(pixel) / 255f
            var g = Color.green(pixel) / 255f
            var b = Color.blue(pixel) / 255f
            r *= expFactor; g *= expFactor; b *= expFactor
            r += a.brightness / 100f; g += a.brightness / 100f; b += a.brightness / 100f
            r = ((r - 0.5f) * contrastFactor + 0.5f)
            g = ((g - 0.5f) * contrastFactor + 0.5f)
            b = ((b - 0.5f) * contrastFactor + 0.5f)
            if (a.lightBalance != 0f) {
                r = r.coerceIn(0f, 1f).pow(gamma)
                g = g.coerceIn(0f, 1f).pow(gamma)
                b = b.coerceIn(0f, 1f).pow(gamma)
            }
            if (a.highlights != 0f) {
                val f = a.highlights / 100f
                r += f * (r - 0.5f).coerceAtLeast(0f)
                g += f * (g - 0.5f).coerceAtLeast(0f)
                b += f * (b - 0.5f).coerceAtLeast(0f)
            }
            if (a.shadows != 0f) {
                val f = a.shadows / 100f
                r += f * (0.5f - r).coerceAtLeast(0f)
                g += f * (0.5f - g).coerceAtLeast(0f)
                b += f * (0.5f - b).coerceAtLeast(0f)
            }
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            r = lum + (r - lum) * satFactor
            g = lum + (g - lum) * satFactor
            b = lum + (b - lum) * satFactor
            r *= 1f + a.temperature / 200f
            b *= 1f - a.temperature / 200f
            g *= 1f + a.tint / 200f
            r *= 1f - a.tint / 400f
            b *= 1f - a.tint / 400f
            if (a.vignette > 0f) {
                val dx = (x - cx) / maxDist
                val dy = (y - cy) / maxDist
                val factor = 1f - (a.vignette / 100f) * (dx * dx + dy * dy) * 1.6f
                r *= factor; g *= factor; b *= factor
            }
            pixels[i] = Color.argb(
                255,
                (r.coerceIn(0f, 1f) * 255).toInt(),
                (g.coerceIn(0f, 1f) * 255).toInt(),
                (b.coerceIn(0f, 1f) * 255).toInt()
            )
        }
    }
    if (a.sharpness > 0f) {
        val amount = a.sharpness / 100f * 0.5f
        val copy = pixels.copyOf()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                for (channel in 0..2) {
                    val shift = 16 - channel * 8
                    val center = (copy[i] shr shift) and 0xFF
                    val sum = ((copy[i - 1] shr shift) and 0xFF) + ((copy[i + 1] shr shift) and 0xFF) +
                        ((copy[i - w] shr shift) and 0xFF) + ((copy[i + w] shr shift) and 0xFF)
                    val value = (center + amount * (4 * center - sum)).toInt().coerceIn(0, 255)
                    pixels[i] = (pixels[i] and (0xFF shl shift).inv()) or (value shl shift)
                }
            }
        }
    }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

fun renderEdited(context: Context, uri: Uri, snapshot: EditSnapshot, maxSize: Int, resizeMax: Int): Bitmap? {
    val source = decodeBitmap(context, uri, maxSize) ?: return null
    var bitmap = applyTransform(source, snapshot.transform)
    if (resizeMax > 0 && (bitmap.width > resizeMax || bitmap.height > resizeMax)) {
        val scale = resizeMax.toFloat() / maxOf(bitmap.width, bitmap.height)
        bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    }
    return applyAdjustments(bitmap, snapshot.adjustments)
}

fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    return stream.toByteArray()
}

fun saveEditedCopy(context: Context, bitmap: Bitmap, originalName: String): Boolean {
    return try {
        val name = originalName.substringBeforeLast(".") + "_edited.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Gallery")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { it.write(bitmapToJpeg(bitmap)) }
        true
    } catch (_: Exception) {
        false
    }
}

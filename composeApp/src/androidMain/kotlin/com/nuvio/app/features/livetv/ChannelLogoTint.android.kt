package com.nuvio.app.features.livetv

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import coil3.request.SuccessResult
import coil3.toBitmap

private const val SAMPLE = 48
private const val HUE_BUCKETS = 24

/**
 * Hue histogram over the opaque, saturated pixels of a small software copy of the logo (coil hands back
 * hardware bitmaps, which can't be read directly). Returns the average colour of the heaviest hue bucket.
 */
internal actual fun channelLogoTint(result: SuccessResult): Color? = runCatching {
    val source = result.image.toBitmap()
    val software = if (source.config == Bitmap.Config.HARDWARE) source.copy(Bitmap.Config.ARGB_8888, false) else source
    val small = Bitmap.createScaledBitmap(software, SAMPLE, SAMPLE, true)
    val pixels = IntArray(SAMPLE * SAMPLE)
    small.getPixels(pixels, 0, SAMPLE, 0, 0, SAMPLE, SAMPLE)
    val weight = DoubleArray(HUE_BUCKETS)
    val sumR = DoubleArray(HUE_BUCKETS)
    val sumG = DoubleArray(HUE_BUCKETS)
    val sumB = DoubleArray(HUE_BUCKETS)
    val hsv = FloatArray(3)
    for (p in pixels) {
        val a = p ushr 24 and 0xFF
        if (a < 200) continue
        val r = p shr 16 and 0xFF
        val g = p shr 8 and 0xFF
        val b = p and 0xFF
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        if (hsv[1] < 0.28f || hsv[2] < 0.22f) continue
        val bucket = ((hsv[0] / 360f) * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
        val w = (hsv[1] * hsv[2]).toDouble()
        weight[bucket] += w
        sumR[bucket] += r * w
        sumG[bucket] += g * w
        sumB[bucket] += b * w
    }
    if (small !== software) small.recycle()
    if (software !== source) software.recycle()
    val best = weight.indices.maxByOrNull { weight[it] } ?: return@runCatching null
    if (weight[best] < 4.0) return@runCatching null
    Color(
        red = (sumR[best] / weight[best] / 255.0).toFloat().coerceIn(0f, 1f),
        green = (sumG[best] / weight[best] / 255.0).toFloat().coerceIn(0f, 1f),
        blue = (sumB[best] / weight[best] / 255.0).toFloat().coerceIn(0f, 1f),
    )
}.getOrNull()

package net.maxsmr.commonutils.graphic

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import java.io.Serializable
import java.util.*

fun generateRandomColor(color: Int): Int {
    val random = Random()
    val red = random.nextInt(256)
    val green = random.nextInt(256)
    val blue = random.nextInt(256)
    return Color.rgb((red + Color.red(color)) / 2,
            (green + Color.green(color)) / 2,
            (blue + Color.blue(color)) / 2)
}

fun generateColorByBitmap(
        bitmap: Bitmap?,
        @ColorInt defaultColor: Int,
        sw: Swatch
): PaletteColors? {
    return if (bitmap != null && isBitmapValid(bitmap)) {
        makePaletteColors(Palette.Builder(bitmap).generate(), defaultColor, sw)
    } else {
        null
    }
}

fun generateColorByBitmapAsync(
        bitmap: Bitmap?,
        @ColorInt defaultColor: Int,
        sw: Swatch,
        listener: (PaletteColors) -> Unit
) {
    if (bitmap != null && isBitmapValid(bitmap)) {
        Palette.Builder(bitmap).generate { palette -> listener.invoke(makePaletteColors(palette, defaultColor, sw)) }
    }
}

/**
 * Resize bitmap if needed
 */
enum class Swatch {
    VIBRANT, LIGHT_VIBRANT, DARK_VIBRANT, MUTED, LIGHT_MUTED, DARK_MUTED
}

data class PaletteColors(
        @ColorInt
        var background: Int = Color.WHITE,
        @ColorInt
        var title: Int = Color.BLACK,
        @ColorInt
        var body: Int = Color.BLACK
) : Serializable

private fun makePaletteColors(palette: Palette?, @ColorInt defaultColor: Int, sw: Swatch?): PaletteColors {
    val colors = PaletteColors()
    if (palette != null && sw != null) {
        when (sw) {
            Swatch.MUTED -> {
                colors.background = palette.getMutedColor(defaultColor)
                colors.title = if (palette.mutedSwatch != null) palette.mutedSwatch!!.titleTextColor else defaultColor
                colors.body = if (palette.mutedSwatch != null) palette.mutedSwatch!!.bodyTextColor else defaultColor
            }
            Swatch.LIGHT_MUTED -> {
                colors.background = palette.getLightMutedColor(defaultColor)
                colors.title = if (palette.lightMutedSwatch != null) palette.lightMutedSwatch!!.titleTextColor else defaultColor
                colors.body = if (palette.lightMutedSwatch != null) palette.lightMutedSwatch!!.bodyTextColor else defaultColor
            }
            Swatch.DARK_MUTED -> {
                colors.background = palette.getDarkMutedColor(defaultColor)
                colors.title = if (palette.darkMutedSwatch != null) palette.darkMutedSwatch!!.titleTextColor else defaultColor
                colors.body = if (palette.darkMutedSwatch != null) palette.darkMutedSwatch!!.bodyTextColor else defaultColor
            }
            Swatch.VIBRANT -> {
                colors.background = palette.getVibrantColor(defaultColor)
                colors.title = if (palette.vibrantSwatch != null) palette.vibrantSwatch!!.titleTextColor else defaultColor
                colors.body = if (palette.vibrantSwatch != null) palette.vibrantSwatch!!.bodyTextColor else defaultColor
            }
            Swatch.LIGHT_VIBRANT -> {
                colors.background = palette.getLightVibrantColor(defaultColor)
                colors.title = if (palette.lightVibrantSwatch != null) palette.lightVibrantSwatch!!.titleTextColor else defaultColor
                colors.body = if (palette.lightVibrantSwatch != null) palette.lightVibrantSwatch!!.bodyTextColor else defaultColor
            }
            Swatch.DARK_VIBRANT -> {
                colors.background = palette.getDarkVibrantColor(defaultColor)
                colors.title = if (palette.darkVibrantSwatch != null) palette.darkVibrantSwatch!!.titleTextColor else defaultColor
                colors.body = if (palette.darkVibrantSwatch != null) palette.darkVibrantSwatch!!.bodyTextColor else defaultColor
            }
        }
    }
    return colors
}
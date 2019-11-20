package net.maxsmr.commonutils.data

import android.text.Html
import android.text.Spanned
import net.maxsmr.commonutils.android.SdkUtils

/**
 * Попытка привести html к строке с потерей части форматирования.
 */
fun String.clearHtml(): String {
    return parseHtmlToSpannedString().toString()
}

/**
 * Попытка привести html к Spanned строке с форматированием из html
 */
fun String.parseHtmlToSpannedString(): Spanned {
    return this.let { this.replace("\n", "<br/>") }
            .let {
                if (SdkUtils.isAtLeastNougat()) {
                    @Suppress("NewApi")
                    Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(it)
                }
            }
}
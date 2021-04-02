package net.maxsmr.commonutils.text

import android.text.Html
import android.text.Spanned
import net.maxsmr.commonutils.isAtLeastNougat

/**
 * Попытка привести html к Spanned строке с форматированием из html
 */
@Throws(Exception::class)
fun CharSequence.parseHtmlToSpannedStringOrThrow(): Spanned =
        toString().replace("\n", "<br/>")
                .let {
                    if (isAtLeastNougat()) {
                        @Suppress("NewApi")
                        Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION")
                        Html.fromHtml(it)
                    }
                }


fun CharSequence?.parseHtmlToSpannedString(): CharSequence = this?.let {
    try {
        parseHtmlToSpannedStringOrThrow()
    } catch (e: Throwable) {
        this
    }
} ?: EMPTY_STRING

/**
 * Попытка привести html к строке с потерей части форматирования.
 */
fun CharSequence?.parseClearedHtml(): String =
        parseHtmlToSpannedString().toString()
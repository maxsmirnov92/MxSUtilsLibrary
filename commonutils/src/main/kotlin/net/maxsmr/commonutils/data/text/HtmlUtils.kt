package net.maxsmr.commonutils.data.text

import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.URLSpan
import net.maxsmr.commonutils.android.SdkUtils

/**
 * Попытка привести html к строке с потерей части форматирования.
 */
fun clearHtml(html: String): String {
    return parseHtmlToSpannedString(html).toString()
}

/**
 * Попытка привести html к Spanned строке с форматированием из html
 */
@Throws
fun parseHtmlToSpannedString(html: String): Spanned {
    return html.let { html.replace("\n", "<br/>") }
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

fun removeUnderline(text: CharSequence): SpannableString {
    val s = SpannableString(text)
    val spans = s.getSpans(0, s.length, URLSpan::class.java)
    for (span in spans) {
        val start = s.getSpanStart(span)
        val end = s.getSpanEnd(span)
        s.removeSpan(span)
        s.setSpan(object : URLSpan(span.url) {

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, start, end, 0)
    }
    return s
}
package net.maxsmr.commonutils.data.text

import android.content.Context
import android.text.*
import android.text.style.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.maxsmr.commonutils.android.SdkUtils
import net.maxsmr.commonutils.android.getBrowseLinkIntent
import net.maxsmr.commonutils.android.gui.SpanInfo

/**
 * Попытка привести html к Spanned строке с форматированием из html
 */
@Throws
fun parseHtmlToSpannedString(html: CharSequence): Spanned {
    return html.let { html.toString().replace("\n", "<br/>") }
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


fun parseHtmlToSpannedStringNoThrow(text: CharSequence): CharSequence {
    return try {
        parseHtmlToSpannedString(text)
    } catch (e: Throwable) {
        text
    }
}

/**
 * Попытка привести html к строке с потерей части форматирования.
 */
fun clearHtml(html: String): String {
    return parseHtmlToSpannedStringNoThrow(html).toString()
}

fun createCustomSpanText(
        text: CharSequence,
        spanInfos: Collection<SpanInfo>
): CharSequence {
    return SpannableStringBuilder(text).apply {
        spanInfos.forEach {
            if (it.startIndex in text.indices && it.endIndex in text.indices) {
                setSpan(createSpan(it.style, it.removeUnderlying, it.clickAction), it.startIndex, it.endIndex, it.flags)
            }
        }
    }
}

fun createCustomSpanTextExpanded(
        text: CharSequence,
        spanInfosMap: Map<SpanInfo, String>
): CharSequence {
    val links = mutableListOf<CharSequence>()
    spanInfosMap.forEach {
        val link = SpannableString(it.value)
        link.setSpan(createSpan(it.key.style, it.key.removeUnderlying, it.key.clickAction), 0, link.length, it.key.flags)
        links.add(link)
    }
    return TextUtils.expandTemplate(SpannableString(text), *links.toTypedArray())
}

fun createLinkableText(
        context: Context,
        text: CharSequence,
        spanInfosMap: Map<SpanInfo, String>
): CharSequence {
    val newSpanInfosMap = mutableListOf<SpanInfo>()
    spanInfosMap.forEach {
        newSpanInfosMap.add(SpanInfo(it.key.startIndex, it.key.endIndex, null, it.key.removeUnderlying, it.key.flags) {
            context.startActivity(getBrowseLinkIntent(it.value))
            // вызываем отдельно исходный action, если он был
            it.key.clickAction?.invoke()
        })
    }
    return createCustomSpanText(text, newSpanInfosMap)
}

fun createSelectedText(
        text: CharSequence,
        @ColorInt highlightColor: Int,
        selection: String,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
): CharSequence {
    return SpannableString(text)
            .apply {
                val start = text.indexOf(selection, ignoreCase = true)
                        .takeIf { it >= 0 }
                        ?: return@apply
                setSpan(
                        ForegroundColorSpan(highlightColor),
                        start,
                        start + selection.length,
                        spanFlags
                )
            }
}

fun appendClickableImage(
        context: Context,
        text: CharSequence,
        @DrawableRes drawableResId: Int,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        clickFunc: () -> Unit = {}
): CharSequence {
    if (text.isNotEmpty()) {
        val s = SpannableString(text)
        val imageSpan = ImageSpan(context, drawableResId, ImageSpan.ALIGN_BASELINE)
        s.setSpan(imageSpan, s.length - 1, s.length, spanFlags)
        s.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                if (!SdkUtils.isPreLollipop()) {
                    widget.cancelPendingInputEvents()
                }
                clickFunc.invoke()
            }
        }, s.length - 1, s.length, spanFlags)
        return s
    }
    return text
}

fun replaceUrlSpans(
        context: Context,
        html: String,
        removeUnderlying: Boolean = true,
        action: ((URLSpan) -> Unit)? = null
): CharSequence {
    val sequence = parseHtmlToSpannedString(html)
    val strBuilder = SpannableStringBuilder(sequence)
    val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
    urls.forEach { span ->
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        strBuilder.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                context.startActivity(getBrowseLinkIntent(span.url))
                action?.invoke(span)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                if (removeUnderlying) {
                    ds.isUnderlineText = false
                }
            }
        }, start, end, flags)
        strBuilder.removeSpan(span)
    }
    return strBuilder
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

private fun createSpan(
        style: CharacterStyle?,
        removeUnderlying: Boolean,
        clickAction: (() -> Unit)?
): CharacterStyle {
    return style
            ?: object : ClickableSpan() {

                override fun onClick(widget: View) {
                    clickAction?.invoke()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    if (removeUnderlying) {
                        ds.isUnderlineText = false
                    }
                }
            }
}
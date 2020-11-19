package net.maxsmr.commonutils.data.text

import android.content.Context
import android.content.Intent
import android.text.*
import android.text.style.*
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.maxsmr.commonutils.android.*
import java.lang.Exception

/**
 * Попытка привести html к Spanned строке с форматированием из html
 */
@Throws(Exception::class)
fun parseHtmlToSpannedStringOrThrow(html: CharSequence): Spanned =
        html.let { html.toString().replace("\n", "<br/>") }
                .let {
                    if (isAtLeastNougat()) {
                        @Suppress("NewApi")
                        Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        @Suppress("DEPRECATION")
                        Html.fromHtml(it)
                    }
                }


fun parseHtmlToSpannedString(text: CharSequence): CharSequence =
        try {
            parseHtmlToSpannedStringOrThrow(text)
        } catch (e: Throwable) {
            text
        }

/**
 * Попытка привести html к строке с потерей части форматирования.
 */
fun clearHtml(html: String): String =
        parseHtmlToSpannedString(html).toString()

fun createCustomSpanText(
        text: CharSequence,
        spanInfos: Collection<SpanInfo>
): CharSequence = SpannableStringBuilder(text).apply {
    spanInfos.forEach {
        if (it.startIndex in text.indices && it.endIndex in 0..text.length) {
            setSpan(createSpan(it.style, it.removeUnderlying, it.clickAction), it.startIndex, it.endIndex, it.flags)
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
        text: CharSequence,
        spanInfosMap: Map<SpanInfo, String>
): CharSequence {
    val newSpanInfosMap = mutableListOf<SpanInfo>()
    spanInfosMap.forEach {
        newSpanInfosMap.add(SpanInfo(it.key.startIndex, it.key.endIndex, object : URLSpan(it.value) {
            override fun onClick(widget: View) {
                super.onClick(widget)
                // вызываем отдельно исходный action, если он был
                it.key.clickAction?.invoke()
            }
        }, it.key.removeUnderlying, it.key.flags))
    }
    return createCustomSpanText(text, newSpanInfosMap)
}

fun createSelectedText(
        text: CharSequence,
        @ColorInt highlightColor: Int,
        selection: String,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
): CharSequence = SpannableString(text)
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
                if (!isPreLollipop()) {
                    widget.cancelPendingInputEvents()
                }
                clickFunc.invoke()
            }
        }, s.length - 1, s.length, spanFlags)
        return s
    }
    return text
}

fun replaceUrlSpansByClickableSpans(
        context: Context,
        html: String,
        removeUnderlying: Boolean = true,
        action: ((URLSpan) -> Boolean)? = null
): CharSequence {
    val sequence = parseHtmlToSpannedStringOrThrow(html)
    val strBuilder = SpannableStringBuilder(sequence)
    val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
    urls.forEach { span ->
        val start = strBuilder.getSpanStart(span)
        val end = strBuilder.getSpanEnd(span)
        val flags = strBuilder.getSpanFlags(span)
        strBuilder.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                if (action?.invoke(span) != true) {
                    // not handled by action
                    startActivitySafe(context, wrapIntent(getBrowseLinkIntent(span.url),
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK))
                }
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
): CharacterStyle = style
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

/**
 * @param style использовать этот стиль; null - использовать [ClickableSpan] с [clickAction]
 */
data class SpanInfo(
        val startIndex: Int,
        val endIndex: Int,
        val style: CharacterStyle? = null,
        val removeUnderlying: Boolean = false,
        val flags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        val clickAction: (() -> Unit)? = null
)
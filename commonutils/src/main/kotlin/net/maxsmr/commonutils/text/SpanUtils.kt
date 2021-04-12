package net.maxsmr.commonutils.text

import android.content.Context
import android.content.Intent
import android.text.*
import android.text.style.*
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.maxsmr.commonutils.*
import kotlin.Pair

/**
 * key: строка, из которой происходит expandTemplate
 * value: строка-url для подстановки
 */
typealias ExpandValueInfo = Pair<String, String>

fun CharSequence.createSpanText(vararg spanInfo: IRangeSpanInfo): CharSequence {
    if (this.isEmpty() || spanInfo.isEmpty()) return this
    return SpannableString(this).apply {
        spanInfo.forEach {
            it.ranges(this@createSpanText).forEach { range ->
                if (range.first in this@createSpanText.indices && range.last in 0..this@createSpanText.length) {
                    setSpan(it.style, range.first, range.last, it.flags)
                }
            }
        }
    }
}

fun CharSequence.createSpanTextExpanded(spanInfoMap: Map<ISpanInfo, String>): CharSequence {
    val parts = mutableListOf<CharSequence>()
    spanInfoMap.forEach {
        val part = SpannableString(it.value)
        part.setSpan(it.key.style, 0, part.length, it.key.flags)
        parts.add(part)
    }
    return TextUtils.expandTemplate(SpannableString(this), *parts.toTypedArray())
}

/**
 * @return [SpannableString], созданная по [spanInfoMap]
 * @param spanInfoMap key - вспомогательная инфа для Span с диапазоном, value - url для перехода
 */
fun CharSequence.createLinkableText(spanInfoMap: Map<IRangeSpanInfo, String>): CharSequence {
    val newSpanInfoMap = mutableListOf<IRangeSpanInfo>()
    spanInfoMap.forEach {
        val ranges = it.key.ranges(this)
        ranges.forEach { range ->
            val span = it.key.style
            newSpanInfoMap.add(RangeSpanInfo(range.first, range.last, UrlClickableSpan(
                    it.value,
                    if (span is AppClickableSpan) span.isUnderlineText else true
            ) { view ->
                if (span is AppClickableSpan) {
                    // вызываем отдельно исходный action, если он был
                    span.onClick?.invoke(view)
                }
            }, it.key.flags))
        }
    }
    return createSpanText(*newSpanInfoMap.toTypedArray())
}

/**
 * @return строка, созданная по [spanInfoMap]
 * @param spanInfoMap key - вспомогательная инфа для Span, value - см. [ExpandValueInfo]
 */
fun CharSequence.createLinkableTextExpanded(spanInfoMap: Map<ISpanInfo, ExpandValueInfo>): CharSequence {
    val newSpanInfoMap = mutableMapOf<ISpanInfo, String>()
    spanInfoMap.entries.forEach {
        val span = it.key.style
        newSpanInfoMap[
                SimpleSpanInfo(
                        UrlClickableSpan(
                                it.value.second,
                                if (span is AppClickableSpan) span.isUnderlineText else true
                        ) { view ->
                            if (span is AppClickableSpan) {
                                // вызываем отдельно исходный action, если он был
                                span.onClick?.invoke(view)
                            }
                        },
                        it.key.flags
                )
        ] = it.value.first
    }
    return createSpanTextExpanded(newSpanInfoMap)
}

@JvmOverloads
fun CharSequence.createSelectedText(
        @ColorInt highlightColor: Int,
        selection: String,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
): CharSequence = createSpanText(
        SubstringSpanInfo(selection,
                ForegroundColorSpan(highlightColor),
                flags = spanFlags)
)

@JvmOverloads
fun CharSequence.appendClickableImage(
        context: Context,
        @DrawableRes drawableResId: Int,
        spanFlags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        clickFunc: () -> Unit = {}
): CharSequence = createSpanText(
        RangeSpanInfo(length - 1, length, ImageSpan(context, drawableResId, ImageSpan.ALIGN_BASELINE), spanFlags),
        RangeSpanInfo(length - 1, length, AppClickableSpan(false) {
            if (!isPreLollipop()) {
                it.cancelPendingInputEvents()
            }
            clickFunc.invoke()
        })
)


@JvmOverloads
fun CharSequence.replaceUrlSpansByClickableSpans(
        parseHtml: Boolean,
        isUnderlineText: Boolean = false,
        action: ((URLSpan) -> Any)? = null
): CharSequence = replaceUrlSpansByCustomSpans(parseHtml) { span ->
    AppClickableSpan(isUnderlineText) {
        action?.invoke(span)
    }
}

@JvmOverloads
fun CharSequence.removeUnderlineFromUrlSpans(
        parseHtml: Boolean,
        action: ((URLSpan) -> Any)? = null
): CharSequence = replaceUrlSpansByCustomSpans(parseHtml) { span ->
    UrlClickableSpan(span.url, false) {
        action?.invoke(span)
    }
}

private fun CharSequence.replaceUrlSpansByCustomSpans(
        parseHtml: Boolean,
        createSpanFunc: (URLSpan) -> CharacterStyle
): SpannableStringBuilder {
    val sequence = if (parseHtml) parseHtmlToSpannedStringOrThrow() else this
    val result = SpannableStringBuilder(sequence)
    val urls = result.getSpans(0, sequence.length, URLSpan::class.java)
    urls.forEach { span ->
        val start = result.getSpanStart(span)
        val end = result.getSpanEnd(span)
        val flags = result.getSpanFlags(span)
        result.removeSpan(span)
        result.setSpan(createSpanFunc(span), start, end, flags)
    }
    return result
}

fun defaultBrowseClickAction(context: Context, url: String) {
    startActivitySafe(context, wrapIntent(getBrowseLinkIntent(url),
            flags = Intent.FLAG_ACTIVITY_NEW_TASK))
}

interface ISpanInfo {

    val style: CharacterStyle
    val flags: Int
}

interface IRangeSpanInfo : ISpanInfo {

    fun ranges(fullText: CharSequence): List<IntRange>
}

/**
 * Простая реализация [ISpanInfo] для случая использования expandTemplate
 */
private data class SimpleSpanInfo(
        override val style: CharacterStyle,
        override val flags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
): ISpanInfo

/**
 * Установить спан [style] в диапазоне [startIndex]..[endIndex] полной строки
 */
data class RangeSpanInfo @JvmOverloads constructor(
        val startIndex: Int,
        val endIndex: Int,
        override val style: CharacterStyle,
        override val flags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
) : IRangeSpanInfo {

    override fun ranges(fullText: CharSequence): List<IntRange> {
        return if (startIndex > endIndex || startIndex !in 0..fullText.length || endIndex !in 0..fullText.length) {
            emptyList()
        } else {
            listOf(IntRange(startIndex, endIndex))
        }
    }
}

/**
 * Установить спан [style] для подстроки [substring] в месте ее нахождения в полной строке
 *
 * @param allEntries true, если спан надо установить для всех вхождений подстроки [substring], false - если для первого
 */
class SubstringSpanInfo @JvmOverloads constructor(
        val substring: String,
        override val style: CharacterStyle,
        val allEntries: Boolean = false,
        override val flags: Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
) : IRangeSpanInfo {

    override fun ranges(fullText: CharSequence): List<IntRange> = if (allEntries) {
        indicesOf(fullText, substring, ignoreCase = true)
                .filter { it > 0 }
                .map { IntRange(it, it + substring.length) }
    } else {
        val start = fullText.indexOf(substring, ignoreCase = true)
        if (start < 0) {
            emptyList()
        } else {
            listOf(IntRange(start, start + substring.length))
        }
    }
}

open class AppClickableSpan @JvmOverloads constructor(
        val isUnderlineText: Boolean = true,
        val onClick: ((View) -> Unit)? = null
) : ClickableSpan() {

    @CallSuper
    override fun onClick(widget: View) {
        onClick?.invoke(widget)
    }

    @CallSuper
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = isUnderlineText
    }
}

open class UrlClickableSpan @JvmOverloads constructor(
        val url: String,
        val isUnderlineText: Boolean = true,
        val onClick: ((View) -> Unit)? = null
) : URLSpan(url) {

    @CallSuper
    override fun onClick(widget: View) {
        onClick?.invoke(widget) ?: super.onClick(widget)
    }

    @CallSuper
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = isUnderlineText
    }
}
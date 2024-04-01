package net.maxsmr.commonutils

import android.content.Context
import android.content.Intent
import android.text.*
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.text.indicesOf
import net.maxsmr.commonutils.text.orEmpty

private const val FLAGS_DEFAULT = Spanned.SPAN_INCLUSIVE_EXCLUSIVE

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("SpanUtils")

/**
 * Попытка привести html к Spanned строке с форматированием из html
 */
@JvmOverloads
fun CharSequence?.parseHtmlToSpannedString(replaceNewLine: Boolean = true): Spanned? = try {
    parseHtmlToSpannedStringOrThrow(replaceNewLine)
} catch (e: Throwable) {
    logger.d(BaseLoggerHolder.formatException(e, "fromHtml"))
    null
}

@Throws(Exception::class)
@JvmOverloads
fun CharSequence?.parseHtmlToSpannedStringOrThrow(replaceNewLine: Boolean = true): Spanned =
    if (replaceNewLine) {
        toString().replace("\n", "<br/>")
    } else {
        this.toString()
    }.let {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            @Suppress("NewApi")
            Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(it)
        }
    }

/**
 * Попытка привести html к строке с потерей части форматирования.
 */
@JvmOverloads
fun CharSequence?.parseClearedHtml(replaceNewLine: Boolean = true): String = parseHtmlToSpannedString(replaceNewLine)?.toString().orEmpty()

/**
 * Добавляет спаны [spanInfo] в строку this.
 *
 * @param spanInfo комбинации данных о позиции для применения и стилях
 */
fun CharSequence.createSpanText(vararg spanInfo: ISpanInfo): CharSequence {
    if (this.isEmpty() || spanInfo.isEmpty()) return this
    return SpannableString(this).also {
        spanInfo.forEach { info ->
            info.ranges(this).forEach { range ->
                if (range.first in this.indices && range.last in 0..this.length) {
                    info.styles.forEach { style ->
                        it.setSpan(style, range.first, range.last, FLAGS_DEFAULT)
                    }
                }
            }
        }
    }
}

/**
 * Аналогичен [TextUtils.expandTemplate] с возможностью установки спанов аргументам [args]
 *
 * @param args Мапа <Аргумент шаблона, Список спанов, применяемых к этому аргументу>
 */
fun CharSequence.createSpanTextExpanded(args: Map<String, List<CharacterStyle>>): CharSequence {
    val parts = args.map { (part, styles) ->
        SpannableString(part).apply {
            styles.forEach {
                setSpan(it, 0, part.length, FLAGS_DEFAULT)
            }
        }
    }.toTypedArray()
    return TextUtils.expandTemplate(SpannableString(this), *parts)
}

@JvmOverloads
fun CharSequence.appendClickableImage(
        context: Context,
        @DrawableRes drawableResId: Int,
        clickFunc: () -> Unit = {}
): CharSequence = createSpanText(
        RangeSpanInfo(length - 1, length, ImageSpan(context, drawableResId, ImageSpan.ALIGN_BASELINE)),
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
        replaceNewLine: Boolean = true,
        action: ((URLSpan) -> Any)? = null
): CharSequence = replaceSpansByCustomSpans(parseHtml, URLSpan::class.java, replaceNewLine) { span ->
    AppClickableSpan(isUnderlineText) { action?.invoke(span) }
}

@JvmOverloads
fun CharSequence.removeUnderlineFromUrlSpans(
        parseHtml: Boolean,
        replaceNewLine: Boolean = true,
        action: ((URLSpan) -> Any)? = null
): CharSequence = replaceSpansByCustomSpans(parseHtml, URLSpan::class.java, replaceNewLine) { span ->
    UrlClickableSpan(span.url, false) { action?.invoke(span) }
}

@JvmOverloads
fun <Style: CharacterStyle> CharSequence.replaceSpansByCustomSpans(
        parseHtml: Boolean,
        clazz: Class<Style>,
        replaceNewLine: Boolean = true,
        createSpanFunc: (Style) -> CharacterStyle,
): SpannableStringBuilder {
    val sequence = if (parseHtml) orEmpty(parseHtmlToSpannedString(replaceNewLine)) else this
    val result = SpannableStringBuilder(sequence)
    val spans = result.getSpans(0, sequence.length, clazz)
    spans.forEach { span ->
        val start = result.getSpanStart(span)
        val end = result.getSpanEnd(span)
        val flags = result.getSpanFlags(span)
        result.removeSpan(span)
        result.setSpan(createSpanFunc(span), start, end, flags)
    }
    return result
}

interface ISpanInfo {

    val styles: List<CharacterStyle>

    fun ranges(fullText: CharSequence): List<IntRange>
}


/**
 * Местоположение для применения стилей указывается диапазоном индексов строки
 */
data class RangeSpanInfo(
        val startIndex: Int,
        val endIndex: Int,
        override val styles: List<CharacterStyle>
) : ISpanInfo {

    constructor(
            startIndex: Int,
            endIndex: Int,
            style: CharacterStyle
    ) : this(startIndex, endIndex, listOf(style))

    override fun ranges(fullText: CharSequence): List<IntRange> {
        return listOfNotNull(
                if (startIndex > endIndex || startIndex !in 0..fullText.length || endIndex !in 0..fullText.length) {
                    null
                } else {
                    IntRange(startIndex, endIndex)
                }
        )
    }
}


/**
 * Устанавливает спаны для подстроки [substring] в месте ее нахождения в полной строке
 */
sealed class SubstringSpanInfo : ISpanInfo {

    abstract val substring: String

    override fun ranges(fullText: CharSequence): List<IntRange> {
        return indices(fullText)
                .filter { it > 0 }
                .map { IntRange(it, it + substring.length) }
    }

    protected abstract fun indices(fullText: CharSequence): List<Int>


    /**
     * Для установки спанов [styles] только для первого вхождения подстроки [substring] в строку
     */
    data class FirstEntry(
            override val substring: String,
            override val styles: List<CharacterStyle>
    ) : SubstringSpanInfo() {

        constructor(substring: String, style: CharacterStyle) : this(substring, listOf(style))

        override fun indices(fullText: CharSequence): List<Int> {
            return listOf(fullText.indexOf(substring, ignoreCase = true))
        }
    }

    /**
     * Для установки спанов, возвращенных [createSpans], для всех вхождений подстроки [substring] в строку
     * Фабрика используется, т.к. для каждого вхождения подстроки в строку андроиду нужен свой
     * инстанс [CharacterStyle].
     */
    class AllEntries(
            override val substring: String,
            private val createSpans: () -> List<CharacterStyle>
    ) : SubstringSpanInfo() {

        override val styles: List<CharacterStyle>
            get() = createSpans()

        override fun indices(fullText: CharSequence): List<Int> {
            return fullText.indicesOf(substring, ignoreCase = true)
        }
    }
}

open class AppClickableSpan @JvmOverloads constructor(
        private val isUnderlineText: Boolean = true,
        private val onClick: ((View) -> Unit)? = null
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
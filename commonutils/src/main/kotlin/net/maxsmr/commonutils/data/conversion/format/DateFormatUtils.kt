package net.maxsmr.commonutils.data.conversion.format

import android.widget.TextView
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.validation.isDateValid
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.MaskFormatWatcher
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

const val MONTH_YEAR_MASK = "__.____"
const val YEAR_MONTH_DAY_MASK = "____.__.__"

const val MONTH_YEAR_DATE_DOTTED_PATTERN = "MM.yyyy"
const val YEAR_MONTH_DAY_DATE_DOTTED_PATTERN = "yyyy.MM.dd"

val MONTH_YEAR_MASK_IMPL: MaskImpl = createMonthYearMask()
val YEAR_MONTH_DAY_MASK_MASK_IMPL: MaskImpl = createYearMonthDayMask()

fun TextView.setMonthYearFormattedText(
        text: CharSequence,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true
) = setFormattedText(text, MONTH_YEAR_MASK_IMPL, applyWatcher, isDistinct) {
    isDateValid(it, MONTH_YEAR_DATE_DOTTED_PATTERN)
}

fun TextView.setYearMonthDayFormattedText(
        text: CharSequence,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true
) = setFormattedText(text, YEAR_MONTH_DAY_MASK_MASK_IMPL, applyWatcher, isDistinct) {
    isDateValid(it, YEAR_MONTH_DAY_DATE_DOTTED_PATTERN)
}

fun parseDateNoThrow(
        dateText: String,
        pattern: String,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): Date? {
    if (pattern.isEmpty()) return null
    return parseDateNoThrow(dateText, SimpleDateFormat(pattern, Locale.getDefault()), dateFormatConfigurator)
}

fun parseDateNoThrow(
        dateText: String,
        dateFormat: SimpleDateFormat,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): Date? {
    with(dateFormat) {
        dateFormatConfigurator?.invoke(this)
        return try {
            parse(dateText)
        } catch (e: ParseException) {
            null
        }
    }
}

fun formatDateNoThrow(
        date: Date,
        pattern: String,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): String {
    if (pattern.isEmpty()) return EMPTY_STRING
    return formatDateNoThrow(date, SimpleDateFormat(pattern, Locale.getDefault()), dateFormatConfigurator)
}

fun formatDateNoThrow(
        date: Date,
        dateFormat: SimpleDateFormat,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): String {
    with(dateFormat) {
        dateFormatConfigurator?.invoke(this)
        return try {
            format(date)
        } catch (e: Exception) {
            EMPTY_STRING
        }
    }
}

fun createMonthYearMask() = MaskImpl.createTerminated(UnderscoreDigitSlotsParser().parseSlots(MONTH_YEAR_MASK))

fun createYearMonthDayMask() = MaskImpl.createTerminated(UnderscoreDigitSlotsParser().parseSlots(YEAR_MONTH_DAY_MASK))

fun createMonthYearWatcher() = MaskFormatWatcher(createMonthYearMask())

fun createYearMonthDayWatcher() = MaskFormatWatcher(createYearMonthDayMask())
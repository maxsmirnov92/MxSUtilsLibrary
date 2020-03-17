package net.maxsmr.commonutils.data.conversion.format

import android.widget.TextView
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

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
) = setFormattedText(text, MONTH_YEAR_MASK_IMPL, applyWatcher, isDistinct)

fun TextView.setYearMonthDayFormattedText(
        text: CharSequence,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true
) = setFormattedText(text, YEAR_MONTH_DAY_MASK_MASK_IMPL, applyWatcher, isDistinct)

fun createMonthYearMask() = MaskImpl.createTerminated(UnderscoreDigitSlotsParser().parseSlots(MONTH_YEAR_MASK))

fun createYearMonthDayMask() = MaskImpl.createTerminated(UnderscoreDigitSlotsParser().parseSlots(YEAR_MONTH_DAY_MASK))

fun createMonthYearWatcher() = MaskFormatWatcher(createMonthYearMask())

fun createYearMonthDayWatcher() = MaskFormatWatcher(createYearMonthDayMask())
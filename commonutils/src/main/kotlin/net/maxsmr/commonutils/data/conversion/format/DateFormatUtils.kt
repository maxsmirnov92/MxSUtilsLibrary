package net.maxsmr.commonutils.data.conversion.format

import net.maxsmr.commonutils.data.text.EMPTY_STRING
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

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

fun getMonthsCount(
        firstTime: Long,
        firstTimeZone: TimeZone = TimeZone.getDefault(),
        secondTime: Long,
        secondTimeZone: TimeZone = TimeZone.getDefault()
) = getMonthsCount(Date(firstTime), firstTimeZone, Date(secondTime), secondTimeZone)

fun getMonthsCount(
        firstDate: Date,
        firstTimeZone: TimeZone = TimeZone.getDefault(),
        secondDate: Date,
        secondTimeZone: TimeZone = TimeZone.getDefault()
): Int {
    val firstCalendar = Calendar.getInstance(firstTimeZone)
    val secondCalendar = Calendar.getInstance(secondTimeZone)
    firstCalendar.time = firstDate
    secondCalendar.time = secondDate
    return getMonthsCount(firstCalendar, secondCalendar)
}

fun getMonthsCount(firstCalendar: Calendar, secondCalendar: Calendar) = firstCalendar.get(Calendar.MONTH) - secondCalendar.get(Calendar.MONTH) + 1 + (firstCalendar.get(Calendar.YEAR) - secondCalendar.get(Calendar.YEAR)) * 12
package net.maxsmr.commonutils.format

import net.maxsmr.commonutils.text.EMPTY_STRING
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@JvmOverloads
fun parseDate(
    dateText: String,
    pattern: String,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone? = null,
    dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): Date? {
    if (pattern.isEmpty()) return null
    return parseDate(dateText, createSdf(pattern, locale, timeZone), dateFormatConfigurator)
}

@JvmOverloads
fun parseDate(
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

@JvmOverloads
fun formatDate(
    date: Date,
    pattern: String,
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone? = null,
    dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): String {
    if (pattern.isEmpty()) return EMPTY_STRING
    return formatDate(date, createSdf(pattern, locale, timeZone), dateFormatConfigurator)
}

@JvmOverloads
fun formatDate(
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

fun getYear(time: String, pattern: String): Int =
    getCalendarField(time, pattern, CalendarUnit.YEAR)

fun getMonth(time: String, pattern: String): Int =
    getCalendarField(time, pattern, CalendarUnit.MONTH)

fun getDay(time: String, pattern: String): Int =
    getCalendarField(time, pattern, CalendarUnit.DAY_OF_MONTH)

fun getHours(time: String, pattern: String): Int =
    getCalendarField(time, pattern, CalendarUnit.HOUR_OF_DAY)

fun getMinutes(time: String, pattern: String): Int =
    getCalendarField(time, pattern, CalendarUnit.MINUTE)

private fun getCalendarField(time: String, pattern: String, unit: CalendarUnit): Int {
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    val date: Date = try {
        simpleDateFormat.parse(time)
    } catch (e: ParseException) {
        return 0
    } ?: return 0

    val calendar: Calendar = getCalendar(date)
    return when (unit) {
        CalendarUnit.YEAR -> calendar[Calendar.YEAR]
        CalendarUnit.MONTH -> calendar[Calendar.MONTH]
        CalendarUnit.DAY_OF_MONTH -> calendar[Calendar.DAY_OF_MONTH]
        CalendarUnit.HOUR_OF_DAY -> calendar[Calendar.HOUR_OF_DAY]
        CalendarUnit.MINUTE -> calendar[Calendar.MINUTE]
    }
}

private fun getCalendar(date: Date): Calendar {
    val c: Calendar = Calendar.getInstance(Locale.getDefault())
    c.time = date
    return c
}

internal enum class CalendarUnit {
    YEAR,
    MONTH,
    DAY_OF_MONTH,
    HOUR_OF_DAY,
    MINUTE
}


private fun createSdf(pattern: String, locale: Locale?, timeZone: TimeZone?) = when {
    locale != null -> {
        SimpleDateFormat(pattern, locale)
    }

    else -> {
        SimpleDateFormat(pattern, Locale.getDefault())
    }
}.apply {
    timeZone?.let {
        setTimeZone(it)
    }
}
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
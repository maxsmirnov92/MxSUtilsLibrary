package net.maxsmr.commonutils.format

import net.maxsmr.commonutils.text.EMPTY_STRING
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun parseDate(
        dateText: String,
        pattern: String,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): Date? {
    if (pattern.isEmpty()) return null
    return parseDate(dateText, SimpleDateFormat(pattern, Locale.getDefault()), dateFormatConfigurator)
}

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

fun formatDate(
        date: Date,
        pattern: String,
        dateFormatConfigurator: ((SimpleDateFormat) -> Unit)? = null
): String {
    if (pattern.isEmpty()) return EMPTY_STRING
    return formatDate(date, SimpleDateFormat(pattern, Locale.getDefault()), dateFormatConfigurator)
}

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
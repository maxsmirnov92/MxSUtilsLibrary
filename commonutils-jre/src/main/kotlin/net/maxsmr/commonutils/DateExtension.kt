package net.maxsmr.commonutils

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun Date.isLess(second: Date?): Boolean {
    second ?: return false
    return this < second // "<"
}

fun Date.isGreater(second: Date?): Boolean {
    second ?: return false
    return this > second // ">"
}

fun Date.isLessOrEquals(second: Date?): Boolean {
    second ?: return false
    return this <= second // "<="
}

fun Date.isGreaterOrEquals(second: Date?): Boolean {
    second ?: return false
    return this >= second // ">="
}

fun Date.isEquals(second: Date?): Boolean {
    second ?: return false
    return this.compareTo(second) == 0 // "=="
}

/** @return true, если эта дата уже прошла (< текущей);
 * считаем, что всегда находимся в начале текущего дня
 */
fun Date.passed() = isLess(Date().toDayStart())

@JvmOverloads
fun Date.toDayStart(locale: Locale? = null, timeZone: TimeZone? = null): Date =
        createCalendar(locale, timeZone).also {
            it.time = this
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
        }.time

@JvmOverloads
fun Date.toDayEnd(locale: Locale? = null, timeZone: TimeZone? = null): Date =
        createCalendar(locale, timeZone).also {
            it.time = this
            it.set(Calendar.HOUR_OF_DAY, 23)
            it.set(Calendar.MINUTE, 59)
            it.set(Calendar.SECOND, 59)
            it.set(Calendar.MILLISECOND, 999)
        }.time

@JvmOverloads
fun Date.toMonthStart(locale: Locale? = null, timeZone: TimeZone? = null): Date =
        createCalendar(locale, timeZone).also {
            it.time = this
            it.set(Calendar.DAY_OF_MONTH, 1)
        }.time

@JvmOverloads
fun Date.getDayOfWeek(locale: Locale? = null, timeZone: TimeZone? = null) = createCalendar(locale, timeZone).getDayOfWeek()

/**
 * Получить номер текущего дня недели (начало с понедельник)
 */
fun Calendar.getDayOfWeek(): Int = 7 - (8 - get(Calendar.DAY_OF_WEEK)) % 7

@JvmOverloads
fun getMonthsCount(
        firstDate: Date,
        secondDate: Date,
        firstLocale: Locale? = null,
        secondLocale: Locale? = null,
        firstTimeZone: TimeZone? = null,
        secondTimeZone: TimeZone? = null
): Int {
    val firstCalendar = createCalendar(firstLocale, firstTimeZone)
    val secondCalendar = createCalendar(secondLocale, secondTimeZone)
    firstCalendar.time = firstDate
    secondCalendar.time = secondDate
    return getMonthsCount(firstCalendar, secondCalendar)
}

fun getMonthsCount(firstCalendar: Calendar, secondCalendar: Calendar) =
        firstCalendar.get(Calendar.MONTH) - secondCalendar.get(Calendar.MONTH) + 1 + (firstCalendar.get(Calendar.YEAR) - secondCalendar.get(Calendar.YEAR)) * 12

fun Date.diff(unit: TimeUnit, time: Long): Date {
    val diff = unit.toMillis(abs(time))
    return Date(if (time > 0) {
        this.time + diff
    } else {
        this.time - diff
    })
}

private fun createCalendar(locale: Locale?, timeZone: TimeZone?): Calendar = when {
    timeZone != null && locale != null -> {
        Calendar.getInstance(timeZone, locale)
    }
    timeZone != null -> {
        Calendar.getInstance(timeZone)
    }
    else -> {
        Calendar.getInstance()
    }
}
package net.maxsmr.commonutils.data.conversion

import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.data.putIfNotNullOrZero
import net.maxsmr.commonutils.data.text.join
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

private const val TIME_UNIT_C0 = 1L
private const val TIME_UNIT_C1 = 1000L
private const val TIME_UNIT_C2 = 1000000L
private const val TIME_UNIT_C3 = 1000000000L
private const val TIME_UNIT_C4 = 60000000000L
private const val TIME_UNIT_C5 = 3600000000000L
private const val TIME_UNIT_C6 = 86400000000000L

/**
 * @param timeUnit           unit for time
 * @param timeUnitsToExclude list of units to avoid in result string
 */
fun timeToString(
        time: Long,
        timeUnit: TimeUnit,
        timeUnitsToExclude: Set<TimeUnit> = setOf(),
        stringsProvider: (Int) -> String
): String {
    val result = mutableListOf<String>()
    val map = timeToMap(time, timeUnit, timeUnitsToExclude)
    val timeFormat = stringsProvider(R.string.time_format)
    map.forEach {
        when (it.key) {
            NANOSECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_nanos)))
            }
            MICROSECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_micros)))
            }
            MILLISECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_millis)))
            }
            SECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_seconds)))
            }
            MINUTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_minutes)))
            }
            HOURS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_hours)))
            }
            DAYS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.time_suffix_days)))
            }
        }
    }
    return join(",", result)
}

fun timeToMap(
        time: Long,
        timeUnit: TimeUnit,
        timeUnitsToExclude: Set<TimeUnit> = setOf()
): Map<TimeUnit, Long> {
    require(time >= 0) { "Incorrect time: $time" }

    val t = timeUnit.toNanos(time)

    val result = sortedMapOf<TimeUnit, Long>()

    if (t < TIME_UNIT_C1 && !timeUnitsToExclude.contains(NANOSECONDS)) {
        putIfNotNullOrZero(result, NANOSECONDS, t)
    } else if ((timeUnitsToExclude.contains(NANOSECONDS) || t in TIME_UNIT_C1 until TIME_UNIT_C2)
            && !timeUnitsToExclude.contains(MICROSECONDS)) {
        val micros = NANOSECONDS.toMicros(t)
        putIfNotNullOrZero(result, MICROSECONDS, micros)
        result.putAll(timeToMapStep(MICROSECONDS.toNanos(micros), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(MICROSECONDS) || t in TIME_UNIT_C2 until TIME_UNIT_C3)
            && !timeUnitsToExclude.contains(MILLISECONDS)) {
        val millis = NANOSECONDS.toMillis(t)
        putIfNotNullOrZero(result, MILLISECONDS, millis)
        result.putAll(timeToMapStep(MILLISECONDS.toNanos(millis), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(MILLISECONDS) || t in TIME_UNIT_C3 until TIME_UNIT_C4)
            && !timeUnitsToExclude.contains(SECONDS)) {
        val seconds = NANOSECONDS.toSeconds(t)
        putIfNotNullOrZero(result, SECONDS, seconds)
        result.putAll(timeToMapStep(SECONDS.toNanos(seconds), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(SECONDS) || t in TIME_UNIT_C4 until TIME_UNIT_C5)
            && !timeUnitsToExclude.contains(MINUTES)) {
        val minutes = NANOSECONDS.toMinutes(t)
        putIfNotNullOrZero(result, MINUTES, minutes)
        result.putAll(timeToMapStep(MINUTES.toNanos(minutes), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(MINUTES) || t in TIME_UNIT_C5 until TIME_UNIT_C6)
            && !timeUnitsToExclude.contains(HOURS)) {
        val hours = NANOSECONDS.toHours(t)
        putIfNotNullOrZero(result, HOURS, hours)
        result.putAll(timeToMapStep(HOURS.toNanos(hours), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(HOURS) || t >= TIME_UNIT_C6)
            && !timeUnitsToExclude.contains(DAYS)) {
        val days = NANOSECONDS.toDays(t)
        putIfNotNullOrZero(result, DAYS, days)
        result.putAll(timeToMapStep(DAYS.toNanos(days), t, timeUnitsToExclude))
    }
    return result
}

private fun timeToMapStep(
        currentNanos: Long,
        sourceNanos: Long,
        timeUnitsToExclude: Set<TimeUnit> = setOf()

): Map<TimeUnit, Long> {
    if (currentNanos > 0) {
        val restNanos = sourceNanos - currentNanos
        if (restNanos > 0) {
            return timeToMap(restNanos, NANOSECONDS, timeUnitsToExclude)
        }
    }
    return emptyMap()
}
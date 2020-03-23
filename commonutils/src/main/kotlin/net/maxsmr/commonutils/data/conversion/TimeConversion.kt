package net.maxsmr.commonutils.data.conversion

import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.data.putIfNotNullOrZero
import net.maxsmr.commonutils.data.text.join
import net.maxsmr.commonutils.data.toSortedSetExclude
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
        stringsProvider: (Int, Long?) -> String
): String {
    val result = mutableListOf<String>()
    val map = timeToMap(time, timeUnit, timeUnitsToExclude)
    val timeFormat = stringsProvider(R.string.time_format, null)
    map.forEach {
        when (it.key) {
            NANOSECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_nanos, it.value)))
            }
            MICROSECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_micros, it.value)))
            }
            MILLISECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_millis, it.value)))
            }
            SECONDS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_seconds, it.value)))
            }
            MINUTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_minutes, it.value)))
            }
            HOURS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_hours, it.value)))
            }
            DAYS -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.time_suffix_days, it.value)))
            }
        }
    }
    return join(", ", result)
}

@JvmOverloads
fun timeToMap(
        time: Long,
        timeUnit: TimeUnit,
        timeUnitsToExclude: Set<TimeUnit> = setOf()
): Map<TimeUnit, Long> {
    require(time >= 0) { "Incorrect time: $time" }

    val t = timeUnit.toNanos(time)

    val result = sortedMapOf<TimeUnit, Long>()

    if (t >= TIME_UNIT_C6 && !timeUnitsToExclude.contains(DAYS)) {
        val days = NANOSECONDS.toDays(t)
        putIfNotNullOrZero(result, DAYS, days)
        result.putAll(timeToMapStep(DAYS.toNanos(days), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(DAYS) || t in TIME_UNIT_C5 until TIME_UNIT_C6)
            && !timeUnitsToExclude.contains(HOURS)) {
        val hours = NANOSECONDS.toHours(t)
        putIfNotNullOrZero(result, HOURS, hours)
        result.putAll(timeToMapStep(HOURS.toNanos(hours), t, toSortedSetExclude(timeUnitsToExclude, setOf(DAYS))))
    } else if ((timeUnitsToExclude.contains(HOURS) || t in TIME_UNIT_C4 until TIME_UNIT_C5)
            && !timeUnitsToExclude.contains(MINUTES)) {
        val minutes = NANOSECONDS.toMinutes(t)
        putIfNotNullOrZero(result, MINUTES, minutes)
        result.putAll(timeToMapStep(MINUTES.toNanos(minutes), t, toSortedSetExclude(timeUnitsToExclude, setOf(HOURS))))
    } else if ((timeUnitsToExclude.contains(MINUTES) || t in TIME_UNIT_C3 until TIME_UNIT_C4)
            && !timeUnitsToExclude.contains(SECONDS)) {
        val seconds = NANOSECONDS.toSeconds(t)
        putIfNotNullOrZero(result, SECONDS, seconds)
        result.putAll(timeToMapStep(SECONDS.toNanos(seconds), t, toSortedSetExclude(timeUnitsToExclude, setOf(MINUTES))))
    } else if ((timeUnitsToExclude.contains(SECONDS) || t in TIME_UNIT_C2 until TIME_UNIT_C3)
            && !timeUnitsToExclude.contains(MILLISECONDS)) {
        val millis = NANOSECONDS.toMillis(t)
        putIfNotNullOrZero(result, MILLISECONDS, millis)
        result.putAll(timeToMapStep(MILLISECONDS.toNanos(millis), t, toSortedSetExclude(timeUnitsToExclude, setOf(SECONDS))))
    } else if ((timeUnitsToExclude.contains(MILLISECONDS) || t in TIME_UNIT_C1 until TIME_UNIT_C2)
            && !timeUnitsToExclude.contains(MICROSECONDS)) {
        val micros = NANOSECONDS.toMicros(t)
        putIfNotNullOrZero(result, MICROSECONDS, micros)
        result.putAll(timeToMapStep(MICROSECONDS.toNanos(micros), t, toSortedSetExclude(timeUnitsToExclude, setOf(MILLISECONDS))))
    } else if (t < TIME_UNIT_C1 && !timeUnitsToExclude.contains(NANOSECONDS)) {
        putIfNotNullOrZero(result, NANOSECONDS, t)
    }

    return result.toSortedMap(reverseOrder())
}

private fun timeToMapStep(
        currentNanos: Long,
        sourceNanos: Long,
        timeUnitsToExclude: Set<TimeUnit>

): Map<TimeUnit, Long> {
    if (currentNanos > 0) {
        val restNanos = sourceNanos - currentNanos
        if (restNanos > 0) {
            return timeToMap(restNanos, NANOSECONDS, timeUnitsToExclude)
        }
    }
    return emptyMap()
}
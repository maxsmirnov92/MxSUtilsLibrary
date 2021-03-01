package net.maxsmr.commonutils.conversion

import net.maxsmr.commonutils.collection.toSortedSetExclude
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

private const val TIME_UNIT_C0 = 1L
private const val TIME_UNIT_C1 = 1000L
private const val TIME_UNIT_C2 = 1000000L
private const val TIME_UNIT_C3 = 1000000000L
private const val TIME_UNIT_C4 = 60000000000L
private const val TIME_UNIT_C5 = 3600000000000L
private const val TIME_UNIT_C6 = 86400000000000L

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
        if (days != 0L) {
            result[DAYS] = days
        }
        result.putAll(timeToMapStep(DAYS.toNanos(days), t, timeUnitsToExclude))
    } else if ((timeUnitsToExclude.contains(DAYS) || t in TIME_UNIT_C5 until TIME_UNIT_C6)
            && !timeUnitsToExclude.contains(HOURS)) {
        val hours = NANOSECONDS.toHours(t)
        if (hours != 0L) {
            result[HOURS] = hours
        }
        result.putAll(timeToMapStep(HOURS.toNanos(hours), t, timeUnitsToExclude.toSortedSetExclude(setOf(DAYS))))
    } else if ((timeUnitsToExclude.contains(HOURS) || t in TIME_UNIT_C4 until TIME_UNIT_C5)
            && !timeUnitsToExclude.contains(MINUTES)) {
        val minutes = NANOSECONDS.toMinutes(t)
        if (minutes != 0L) {
            result[MINUTES] = minutes
        }
        result.putAll(timeToMapStep(MINUTES.toNanos(minutes), t, timeUnitsToExclude.toSortedSetExclude(setOf(HOURS))))
    } else if ((timeUnitsToExclude.contains(MINUTES) || t in TIME_UNIT_C3 until TIME_UNIT_C4)
            && !timeUnitsToExclude.contains(SECONDS)) {
        val seconds = NANOSECONDS.toSeconds(t)
        if (seconds != 0L) {
            result[SECONDS] = seconds
        }
        result.putAll(timeToMapStep(SECONDS.toNanos(seconds), t, timeUnitsToExclude.toSortedSetExclude(setOf(MINUTES))))
    } else if ((timeUnitsToExclude.contains(SECONDS) || t in TIME_UNIT_C2 until TIME_UNIT_C3)
            && !timeUnitsToExclude.contains(MILLISECONDS)) {
        val millis = NANOSECONDS.toMillis(t)
        if (millis != 0L) {
            result[MILLISECONDS] = millis
        }
        result.putAll(timeToMapStep(MILLISECONDS.toNanos(millis), t, timeUnitsToExclude.toSortedSetExclude(setOf(SECONDS))))
    } else if ((timeUnitsToExclude.contains(MILLISECONDS) || t in TIME_UNIT_C1 until TIME_UNIT_C2)
            && !timeUnitsToExclude.contains(MICROSECONDS)) {
        val micros = NANOSECONDS.toMicros(t)
        if (micros != 0L) {
            result[MICROSECONDS] = micros
        }
        result.putAll(timeToMapStep(MICROSECONDS.toNanos(micros), t, timeUnitsToExclude.toSortedSetExclude(setOf(MILLISECONDS))))
    } else if (t < TIME_UNIT_C1 && !timeUnitsToExclude.contains(NANOSECONDS)) {
        if (t != 0L) {
            result[NANOSECONDS] = t
        }
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
package net.maxsmr.commonutils.format

import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.conversion.timeToMap
import net.maxsmr.commonutils.text.join
import java.util.concurrent.TimeUnit

private const val FORMAT_TIME = "%s %s"

/**
 * @param timeUnit           unit for time
 * @param timeUnitsToExclude list of units to avoid in result string
 */
@JvmOverloads
fun timeToString(
        time: Long,
        timeUnit: TimeUnit,
        timeUnitsToExclude: Set<TimeUnit> = setOf(),
        stringsProvider: (Int, Long?) -> String
): String {
    val result = mutableListOf<String>()
    val map = timeToMap(time, timeUnit, timeUnitsToExclude)
    map.forEach {
        when (it.key) {
            TimeUnit.NANOSECONDS -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_nanos, it.value)))
            }
            TimeUnit.MICROSECONDS -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_micros, it.value)))
            }
            TimeUnit.MILLISECONDS -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_millis, it.value)))
            }
            TimeUnit.SECONDS -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_seconds, it.value)))
            }
            TimeUnit.MINUTES -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_minutes, it.value)))
            }
            TimeUnit.HOURS -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_hours, it.value)))
            }
            TimeUnit.DAYS -> {
                result.add(String.format(FORMAT_TIME, it.value, stringsProvider(R.plurals.time_suffix_days, it.value)))
            }
        }
    }
    return join(", ", result)
}
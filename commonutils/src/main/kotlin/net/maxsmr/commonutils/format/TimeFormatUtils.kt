package net.maxsmr.commonutils.format

import androidx.annotation.PluralsRes
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.conversion.decomposeTime
import net.maxsmr.commonutils.conversion.decomposeTimeSingle
import net.maxsmr.commonutils.gui.message.PluralTextMessage
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

/**
 * Исключать из декомпозиции единицы (будет в результате, если единственная):
 */
@JvmField
val TIME_UNITS_TO_EXCLUDE_DEFAULT = setOf(SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS)

@JvmOverloads
fun decomposeTimeSingleFormatted(
    time: Long,
    timeUnit: TimeUnit,
    pluralFormat: TimePluralFormat,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
): PluralTextMessage? {
    return decomposeTimeSingleFormatted(
        time,
        timeUnit,
        pluralFormat,
        ignoreExclusionIfOnly,
        timeUnitsToExclude
    ) { resId, value ->
        toPluralTextMessage(resId, value, pluralFormat.isWithValue)
    }
}

@JvmOverloads
fun <F> decomposeTimeSingleFormatted(
    time: Long,
    timeUnit: TimeUnit,
    pluralFormat: TimePluralFormat,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
    formatFunc: (Int, Long) -> F,
): F? {
    val pair = decomposeTimeSingle(time, timeUnit, ignoreExclusionIfOnly, timeUnitsToExclude)
    return decomposeTimeSingleFormatted(pair, pluralFormat, formatFunc)
}

fun decomposeTimeSingleFormatted(
    time: Pair<Long, TimeUnit>?,
    pluralFormat: TimePluralFormat,
): PluralTextMessage? {
    return decomposeTimeSingleFormatted(
        time,
        pluralFormat
    ) { resId, value ->
        toPluralTextMessage(resId, value, pluralFormat.isWithValue)
    }
}

fun <F> decomposeTimeSingleFormatted(
    time: Pair<Long, TimeUnit>?,
    pluralFormat: TimePluralFormat,
    formatFunc: (Int, Long) -> F,
): F? {
    if (time == null) {
        return null
    }
    val value = time.first
    val pluralResId = pluralFormat.getPluralTextResId(time.second)
    return formatFunc(pluralResId, value)
}

@JvmOverloads
fun decomposeTimeFormatted(
    time: Long,
    timeUnit: TimeUnit,
    pluralFormat: TimePluralFormat,
    emptyIfZero: Boolean = true,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
): List<PluralTextMessage> {
    return decomposeTimeFormatted(time,
        timeUnit,
        pluralFormat,
        emptyIfZero,
        ignoreExclusionIfOnly,
        timeUnitsToExclude,
        formatFunc = { resId, value ->
            toPluralTextMessage(resId, value, pluralFormat.isWithValue)
        })
}

fun decomposeTimeFormatted(
    decomposedMap: Map<TimeUnit, Long>,
    pluralFormat: TimePluralFormat,
): List<PluralTextMessage> {
    return decomposeTimeFormatted(decomposedMap, pluralFormat, formatFunc = { resId, value ->
        toPluralTextMessage(resId, value, pluralFormat.isWithValue)
    })
}

@JvmOverloads
fun <F> decomposeTimeFormatted(
    time: Long,
    timeUnit: TimeUnit,
    pluralFormat: TimePluralFormat,
    emptyIfZero: Boolean = true,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
    formatFunc: (Int, Long) -> F,
): List<F> {
    val map = decomposeTime(time, timeUnit, emptyIfZero, ignoreExclusionIfOnly, timeUnitsToExclude)
    return decomposeTimeFormatted(map, pluralFormat, formatFunc)
}

fun <F> decomposeTimeFormatted(
    decomposedMap: Map<TimeUnit, Long>,
    pluralFormat: TimePluralFormat,
    formatFunc: (Int, Long) -> F,
): List<F> {
    val result = mutableListOf<F>()
    decomposedMap.forEach {
        val pluralResId = pluralFormat.getPluralTextResId(it.key)
        result.add(formatFunc(pluralResId, it.value))
    }
    return result
}

fun toPluralTextMessage(
    @PluralsRes pluralResId: Int,
    value: Long,
    formatWithValue: Boolean,
): PluralTextMessage {
    val intValue = value.toInt()
    return if (formatWithValue) {
        PluralTextMessage(pluralResId, intValue, intValue)
    } else {
        PluralTextMessage(pluralResId, intValue)
    }
}

enum class TimePluralFormat {

    NORMAL,
    NORMAL_WITH_VALUE,
    DECLENSION,
    DECLENSION_WITH_VALUE;

    val isWithValue: Boolean get() = this == NORMAL_WITH_VALUE || this == DECLENSION_WITH_VALUE

    @PluralsRes
    internal fun getPluralTextResId(timeUnit: TimeUnit): Int {
        return when (timeUnit) {
            NANOSECONDS -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_nanos
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_nanos_format
                    DECLENSION -> R.plurals.time_unit_nanos_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_nanos_declension_format
                }
            }
            MICROSECONDS -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_micros
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_micros_format
                    DECLENSION -> R.plurals.time_unit_micros_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_micros_declension_format
                }
            }
            MILLISECONDS -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_millis
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_millis_format
                    DECLENSION -> R.plurals.time_unit_millis_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_millis_declension_format
                }
            }
            SECONDS -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_seconds
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_seconds_format
                    DECLENSION -> R.plurals.time_unit_seconds_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_seconds_declension_format
                }
            }
            MINUTES -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_minutes
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_minutes_format
                    DECLENSION -> R.plurals.time_unit_minutes_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_minutes_declension_format
                }
            }
            HOURS -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_hours
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_hours_format
                    DECLENSION -> R.plurals.time_unit_hours_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_hours_declension_format
                }
            }
            DAYS -> {
                when (this) {
                    NORMAL -> R.plurals.time_unit_days
                    NORMAL_WITH_VALUE -> R.plurals.time_unit_days_format
                    DECLENSION -> R.plurals.time_unit_days_declension
                    DECLENSION_WITH_VALUE -> R.plurals.time_unit_days_declension_format
                }
            }
        }
    }
}
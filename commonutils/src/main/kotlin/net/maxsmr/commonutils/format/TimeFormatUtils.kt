package net.maxsmr.commonutils.format

import android.icu.text.PluralFormat
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.conversion.decomposeTime
import net.maxsmr.commonutils.conversion.decomposeTimeSingle
import net.maxsmr.commonutils.gui.actions.message.text.PluralTextMessage
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*

/**
 * Исключать из декомпозиции единицы (будет в результате, если единственная):
 */
@JvmField
val TIME_UNITS_TO_EXCLUDE_DEFAULT = setOf(SECONDS, MILLISECONDS, MICROSECONDS, NANOSECONDS)

/**
 * Округлять только:
 */
@JvmField
val TIME_UNITS_TO_INCLUDE_ROUND_DEFAULT = setOf(MINUTES)

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
        toPluralTextMessage(resId, value, pluralFormat.isFormatted)
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
        toPluralTextMessage(resId, value, pluralFormat.isFormatted)
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
    emptyMapIfZero: Boolean = true,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
): List<PluralTextMessage> {
    return decomposeTimeFormatted(time,
        timeUnit,
        pluralFormat,
        emptyMapIfZero,
        ignoreExclusionIfOnly,
        timeUnitsToExclude,
        formatFunc = { resId, value ->
            toPluralTextMessage(resId, value, pluralFormat.isFormatted)
        })
}

fun decomposeTimeFormatted(
    decomposedMap: Map<TimeUnit, Long>,
    pluralFormat: TimePluralFormat,
): List<PluralTextMessage> {
    return decomposeTimeFormatted(decomposedMap, pluralFormat, formatFunc = { resId, value ->
        toPluralTextMessage(resId, value, pluralFormat.isFormatted)
    })
}

@JvmOverloads
fun <F> decomposeTimeFormatted(
    time: Long,
    timeUnit: TimeUnit,
    pluralFormat: TimePluralFormat,
    emptyMapIfZero: Boolean = true,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
    formatFunc: (Int, Long) -> F,
): List<F> {
    val map = decomposeTime(time, timeUnit, emptyMapIfZero, ignoreExclusionIfOnly, timeUnitsToExclude)
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

@StringRes
private fun TimePluralFormat.getPluralTextResId(timeUnit: TimeUnit): Int {
    return when (timeUnit) {
        NANOSECONDS -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_nanos
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_nanos_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_nanos_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_nanos_declension_format
            }
        }
        MICROSECONDS -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_micros
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_micros_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_micros_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_micros_declension_format
            }
        }
        MILLISECONDS -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_millis
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_millis_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_millis_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_millis_declension_format
            }
        }
        SECONDS -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_seconds
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_seconds_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_seconds_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_seconds_declension_format
            }
        }
        MINUTES -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_minutes
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_minutes_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_minutes_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_minutes_declension_format
            }
        }
        HOURS -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_hours
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_hours_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_hours_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_hours_declension_format
            }
        }
        DAYS -> {
            when (this) {
                TimePluralFormat.NORMAL -> R.plurals.time_unit_days
                TimePluralFormat.NORMAL_FORMAT -> R.plurals.time_unit_days_format
                TimePluralFormat.DECLENSION -> R.plurals.time_unit_days_declension
                TimePluralFormat.DECLENSION_FORMAT -> R.plurals.time_unit_days_declension_format
            }
        }
    }
}

private fun toPluralTextMessage(
    @PluralsRes pluralResId: Int,
    value: Long,
    isFormattedString: Boolean,
): PluralTextMessage {
    val intValue = value.toInt()
    return if (isFormattedString) {
        PluralTextMessage(pluralResId, intValue, intValue)
    } else {
        PluralTextMessage(pluralResId, intValue)
    }
}

enum class TimePluralFormat {

    NORMAL,
    NORMAL_FORMAT,
    DECLENSION,
    DECLENSION_FORMAT;

    val isFormatted: Boolean get() = this == NORMAL_FORMAT || this == DECLENSION_FORMAT
}
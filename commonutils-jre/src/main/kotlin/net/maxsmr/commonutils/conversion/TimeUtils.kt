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
fun roundTimeIncluded(
    time: Long,
    timeUnit: TimeUnit,
    rule: TimeRoundRule = TimeRoundRule.MORE,
    timeUnitsToInclude: Set<TimeUnit> = TimeUnit.values().toSet(),
): Long {
    return roundTime(time,
        timeUnit,
        rule,
        if (timeUnitsToInclude.isEmpty()) setOf() else values().toSet() - timeUnitsToInclude)
}

/**
 * @return округленное время в [TimeUnit.NANOSECONDS]
 */
@JvmOverloads
fun roundTime(
    time: Long,
    timeUnit: TimeUnit,
    rule: TimeRoundRule = TimeRoundRule.MORE,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
): Long {
    var result: Long = timeUnit.toNanos(time)
    if (!timeUnitsToExclude.contains(MICROSECONDS)) {
        val micros = NANOSECONDS.toMicros(result)
        val restNanos = result - MICROSECONDS.toNanos(micros)
        result = when (rule.compare(restNanos, 500)) {
            1 -> MICROSECONDS.toNanos(micros + 1)
            -1 -> MICROSECONDS.toNanos(micros)
            else -> result
        }
    }
    if (!timeUnitsToExclude.contains(MILLISECONDS)) {
        val millis = NANOSECONDS.toMillis(result)
        val restMicros = NANOSECONDS.toMicros(result) - MILLISECONDS.toMicros(millis)
        result = when (rule.compare(restMicros, 500)) {
            1 -> MILLISECONDS.toNanos(millis + 1)
            -1 -> MILLISECONDS.toNanos(millis)
            else -> result
        }
    }
    if (!timeUnitsToExclude.contains(SECONDS)) {
        val seconds = NANOSECONDS.toSeconds(result)
        val restMillis = NANOSECONDS.toMillis(result) - SECONDS.toMillis(seconds)
        result = when (rule.compare(restMillis, 500)) {
            1 -> SECONDS.toNanos(seconds + 1)
            -1 -> SECONDS.toNanos(seconds)
            else -> result
        }
    }
    if (!timeUnitsToExclude.contains(MINUTES)) {
        val minutes = NANOSECONDS.toMinutes(result)
        val restSeconds = NANOSECONDS.toSeconds(result) - MINUTES.toSeconds(minutes)
        result = when (rule.compare(restSeconds, 30)) {
            1 -> MINUTES.toNanos(minutes + 1)
            -1 -> MINUTES.toNanos(minutes)
            else -> result
        }
    }
    if (!timeUnitsToExclude.contains(HOURS)) {
        val hours = NANOSECONDS.toHours(result)
        val restMinutes = NANOSECONDS.toMinutes(result) - HOURS.toMinutes(hours)
        result = when (rule.compare(restMinutes, 30)) {
            1 -> HOURS.toNanos(hours + 1)
            -1 -> HOURS.toNanos(hours)
            else -> result
        }
    }
    if (!timeUnitsToExclude.contains(DAYS)) {
        val days = NANOSECONDS.toDays(result)
        val restHours = NANOSECONDS.toHours(result) - DAYS.toHours(days)
        result = when (rule.compare(restHours, 12)) {
            1 -> DAYS.toNanos(days + 1)
            -1 -> DAYS.toNanos(days)
            else -> result
        }
    }
    return result
}

enum class TimeRoundRule {

    /**
     * Округлять в любую сторону
     */
    ALL,

    /**
     * Только в меньшую
     */
    LESS,

    /**
     * Только в большую
     */
    MORE;

    fun compare(value: Long, target: Long): Int {
        return if (value > target && (this == ALL || this == MORE)) {
            1
        } else if (value in 1..target && (this == ALL || this == LESS)) {
            -1
        } else {
            0
        }
    }
}

fun decomposeTimeSingleIncluded(
    time: Long,
    timeUnit: TimeUnit,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToInclude: Set<TimeUnit> = TimeUnit.values().toSet(),
): Pair<Long, TimeUnit>? {
    return decomposeTimeSingle(time,
        timeUnit,
        ignoreExclusionIfOnly,
        if (timeUnitsToInclude.isEmpty()) setOf() else values().toSet() - timeUnitsToInclude)
}

fun decomposeTimeSingle(
    time: Long,
    timeUnit: TimeUnit,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
): Pair<Long, TimeUnit>? {
    if (time < 0) return null
    val days = timeUnit.toDays(time)
    return if (days > 0 && !timeUnitsToExclude.contains(DAYS)) {
        Pair(days, DAYS)
    } else {
        val hours = timeUnit.toHours(time)
        if (hours > 0 && !timeUnitsToExclude.contains(HOURS)) {
            Pair(hours, HOURS)
        } else {
            val minutes = timeUnit.toMinutes(time)
            if (minutes > 0 && !timeUnitsToExclude.contains(MINUTES)) {
                Pair(minutes, MINUTES)
            } else {
                val seconds = timeUnit.toSeconds(time)
                if (seconds > 0 && !timeUnitsToExclude.contains(SECONDS)) {
                    Pair(minutes, SECONDS)
                } else {
                    val millis = timeUnit.toMillis(time)
                    if (millis > 0 && !timeUnitsToExclude.contains(MILLISECONDS)) {
                        Pair(millis, MILLISECONDS)
                    } else {
                        val micros = timeUnit.toMicros(time)
                        if (micros > 0 && !timeUnitsToExclude.contains(MICROSECONDS)) {
                            Pair(micros, MICROSECONDS)
                        } else {
                            val nanos = timeUnit.toNanos(time)
                            if (nanos > 0 && (!timeUnitsToExclude.contains(NANOSECONDS) || ignoreExclusionIfOnly)) {
                                Pair(nanos, NANOSECONDS)
                            } else {
                                null
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Позволяет разложить время [time] в заданных единицах измерения на требуемые единицы измерения [timeUnitsToInclude].
 * Если число единиц любого из запрошенных [timeUnitsToInclude] равно 0, то TimeUnit не будет включен в результат
 * (требуется проверять на null в месте вызова функции).
 *
 * @param time количество единиц исходного времени
 * @param timeUnit единицы измерения исходного времени [time]
 * @param emptyMapIfZero вернуть пустую мапу при true, если нет единиц; 0 и наиболее крупная допустимая единица - в противном случае
 * @param ignoreExclusionIfOnly true, если проверяемая единица единственная и находится в исключениях - игнорировать
 * @param timeUnitsToInclude требуемые единицы для разложения. Если отсутствует, разложение производится во всем возможным единицам
 * @return мапа <единица измерения, количество единиц>
 */
@JvmOverloads
fun decomposeTimeIncluded(
    time: Long,
    timeUnit: TimeUnit,
    emptyMapIfZero: Boolean = true,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToInclude: Set<TimeUnit> = TimeUnit.values().toSet(),
): Map<TimeUnit, Long> {
    return decomposeTime(time,
        timeUnit,
        emptyMapIfZero,
        ignoreExclusionIfOnly,
        if (timeUnitsToInclude.isEmpty()) setOf() else values().toSet() - timeUnitsToInclude)
}

@JvmOverloads
fun decomposeTime(
    time: Long,
    timeUnit: TimeUnit,
    emptyMapIfZero: Boolean = true,
    ignoreExclusionIfOnly: Boolean = true,
    timeUnitsToExclude: Set<TimeUnit> = setOf(),
): Map<TimeUnit, Long> {
    val result = decomposeTime(time, timeUnit, ignoreExclusionIfOnly, timeUnitsToExclude, setOf()).toMutableMap()
    if (!emptyMapIfZero && result.isEmpty() && timeUnitsToExclude.isNotEmpty()) {
        val units = TimeUnit.values().toSortedSet(reverseOrder()).filter {
            !timeUnitsToExclude.contains(it)
        }
        units.getOrNull(0)?.let {
            result.put(it, 0)
        }
    }
    return result
}

/**
 * @param alreadyDecomposedUnits единицы, уже включённые в результат
 */
private fun decomposeTime(
    time: Long,
    timeUnit: TimeUnit,
    ignoreExclusionIfOnly: Boolean,
    timeUnitsToExclude: Set<TimeUnit>,
    alreadyDecomposedUnits: Set<TimeUnit>,
): Map<TimeUnit, Long> {
    if (time < 0) return mapOf()

    val t = timeUnit.toNanos(time)

    fun TimeUnit.isInRange() = when (this) {
        DAYS -> t >= TIME_UNIT_C6
        HOURS -> t in TIME_UNIT_C5 until TIME_UNIT_C6
        MINUTES -> t in TIME_UNIT_C4 until TIME_UNIT_C5
        SECONDS -> t in TIME_UNIT_C3 until TIME_UNIT_C4
        MILLISECONDS -> t in TIME_UNIT_C2 until TIME_UNIT_C3
        MICROSECONDS -> t in TIME_UNIT_C1 until TIME_UNIT_C2
        NANOSECONDS -> t < TIME_UNIT_C1
    }

    fun TimeUnit.isInRangeOrExcluded(): Boolean {
        // следующая по крупности от этой единица проверяется на вхождение в исключения
        val biggerUnit = TimeUnit.values().getOrNull(this.ordinal + 1)
        return this != DAYS && this != NANOSECONDS && biggerUnit != null && timeUnitsToExclude.contains(biggerUnit)
                // ИЛИ эта находится в диапазоне - необходимое и достаточное условие
                || isInRange()
    }

    fun TimeUnit.checkAcceptable(): Boolean {
        if (!this.isInRangeOrExcluded()) {
            return false
        }
        val smallerUnits = TimeUnit.values().filter {
            // более мелкая единица по отношению к этой, не в исключениях
            if (!(it.ordinal < this.ordinal
                        && !timeUnitsToExclude.contains(it))
            ) {
                return@filter false
            }
            // текущее значение попадает в диапазон предыдущей единицы
            val smallerUnit = TimeUnit.values().getOrNull(this.ordinal - 1)
            smallerUnit?.isInRangeOrExcluded() ?: false
        }
        // данная единица не в исключениях - необходимое и достаточное условие
        return !timeUnitsToExclude.contains(this)
                // ИЛИ она в исключениях, НО:
                || ignoreExclusionIfOnly &&
                ( // отсутствуют: ранее заполненные
                        alreadyDecomposedUnits.isEmpty()
                                // И более мелкие единицы НЕ в исключениях
                                // для попадания в мапу на следующих decomposeTime
                                && smallerUnits.isEmpty()
                        )
    }

    val result = sortedMapOf<TimeUnit, Long>()

    if (DAYS.checkAcceptable()) {
        val days = NANOSECONDS.toDays(t)
        if (days != 0L) {
            result[DAYS] = days
        }
        result.putAll(timeToMapStep(
            DAYS.toNanos(days),
            t,
            timeUnitsToExclude,
            ignoreExclusionIfOnly,
            result.keys))
    } else if (HOURS.checkAcceptable()) {
        val hours = NANOSECONDS.toHours(t)
        if (hours != 0L) {
            result[HOURS] = hours
        }
        // убираем из исключений следующую по крупности единицу, чтобы ещё раз не попасть сюда же
        result.putAll(timeToMapStep(HOURS.toNanos(hours),
            t,
            timeUnitsToExclude.toSortedSetExclude(setOf(DAYS)),
            ignoreExclusionIfOnly,
            result.keys))
    } else if (MINUTES.checkAcceptable()) {
        val minutes = NANOSECONDS.toMinutes(t)
        if (minutes != 0L) {
            result[MINUTES] = minutes
        }
        result.putAll(timeToMapStep(MINUTES.toNanos(minutes),
            t,
            timeUnitsToExclude.toSortedSetExclude(setOf(HOURS)),
            ignoreExclusionIfOnly,
            result.keys))
    } else if (SECONDS.checkAcceptable()) {
        val seconds = NANOSECONDS.toSeconds(t)
        if (seconds != 0L) {
            result[SECONDS] = seconds
        }
        result.putAll(timeToMapStep(SECONDS.toNanos(seconds),
            t,
            timeUnitsToExclude.toSortedSetExclude(setOf(MINUTES)),
            ignoreExclusionIfOnly,
            result.keys))
    } else if (MILLISECONDS.checkAcceptable()) {
        val millis = NANOSECONDS.toMillis(t)
        if (millis != 0L) {
            result[MILLISECONDS] = millis
        }
        result.putAll(timeToMapStep(MILLISECONDS.toNanos(millis),
            t,
            timeUnitsToExclude.toSortedSetExclude(setOf(SECONDS)),
            ignoreExclusionIfOnly,
            result.keys))
    } else if (MICROSECONDS.checkAcceptable()) {
        val micros = NANOSECONDS.toMicros(t)
        if (micros != 0L) {
            result[MICROSECONDS] = micros
        }
        result.putAll(timeToMapStep(MICROSECONDS.toNanos(micros),
            t,
            timeUnitsToExclude.toSortedSetExclude(setOf(MILLISECONDS)),
            ignoreExclusionIfOnly,
            result.keys))
    } else if (NANOSECONDS.checkAcceptable()) {
        if (t != 0L) {
            result[NANOSECONDS] = t
        }
    }
    return result.toSortedMap(reverseOrder())
}

private fun timeToMapStep(
    currentNanos: Long,
    sourceNanos: Long,
    timeUnitsToExclude: Set<TimeUnit>,
    ignoreExclusionIfOnly: Boolean,
    alreadyDecomposedUnits: Set<TimeUnit>,
): Map<TimeUnit, Long> {
    val restNanos = sourceNanos - currentNanos
    if (restNanos > 0) {
        return decomposeTime(restNanos,
            NANOSECONDS,
            ignoreExclusionIfOnly,
            timeUnitsToExclude,
            alreadyDecomposedUnits)
    }
    return emptyMap()
}

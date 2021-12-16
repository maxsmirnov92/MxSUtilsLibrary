package net.maxsmr.commonutils.datetime

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import org.joda.time.*
import java.util.*

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("JodaTimeUtils")

/**
 * Время между [targetDate] и текущим временем
 * @param isBefore при true предполагается, что сравниваемое время должно быть в прошлом
 */
@JvmOverloads
fun timeBetweenCurrent(
    targetDate: Date,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenCurrentOrThrow(targetDate, unit, isBefore)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenCurrentOrThrow(
    targetDate: Date,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int {
    val currentTime = System.currentTimeMillis()
    return timeBetweenOrThrow(targetDate.time, currentTime, unit, isBefore)
}

@JvmOverloads
fun timeBetween(
    firstMillis: Long,
    secondMillis: Long,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenOrThrow(firstMillis, secondMillis, unit, isBefore)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenOrThrow(
    firstMillis: Long,
    secondMillis: Long,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int = timeBetweenOrThrow(Date(firstMillis), Date(secondMillis), unit, isBefore)

@JvmOverloads
fun timeBetween(
    firstDate: Date,
    secondDate: Date,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenOrThrow(firstDate, secondDate, unit, isBefore)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenOrThrow(
    firstDate: Date,
    secondDate: Date,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int {

    fun Date.fromDateFields() = try {
        LocalDate.fromDateFields(this)
    } catch (e: RuntimeException) {
        throw RuntimeException(formatException(e, "fromDateFields for date '$this'"))
    }

    val one = firstDate.fromDateFields()
    val other = secondDate.fromDateFields()

    val first: LocalDate
    val second: LocalDate
    val isSecondAfter = secondDate.time > firstDate.time
    if (isSecondAfter) {
        first = one
        second = other
    } else {
        first = other
        second = one
    }
    val diff: Int = try {
        when (unit) {
            JodaTimeUnit.YEARS -> Years.yearsBetween(first, second).years
            JodaTimeUnit.MONTHS -> Months.monthsBetween(first, second).months
            JodaTimeUnit.WEEKS -> Weeks.weeksBetween(first, second).weeks
            JodaTimeUnit.DAYS -> Days.daysBetween(first, second).days
        }
    } catch (e: RuntimeException) {
        throw RuntimeException(formatException(e, "org.joda.time between"))
    }
    return if (isSecondAfter) {
        if (isBefore) {
            diff
        } else {
            -diff
        }
    } else {
        if (isBefore) {
            -diff
        } else {
            diff
        }
    }
}

enum class JodaTimeUnit {
    DAYS,
    MONTHS,
    WEEKS,
    YEARS
}
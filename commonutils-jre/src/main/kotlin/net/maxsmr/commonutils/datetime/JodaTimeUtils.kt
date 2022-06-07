package net.maxsmr.commonutils.datetime

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.text.EMPTY_STRING
import org.joda.time.*
import org.joda.time.base.BaseLocal
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("JodaTimeUtils")

const val DEFAULT_DATE_PATTERN = "dd.MM.yyyy"

/**
 * Время между [targetDateTimeFormatted] и текущим временем
 * @param isBefore при true предполагается, что сравниваемое время должно быть в прошлом
 */
@JvmOverloads
fun timeBetweenCurrent(
    targetDateTimeFormatted: String,
    unit: JodaTimeUnit,
    pattern: String = DEFAULT_DATE_PATTERN,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenCurrentOrThrow(targetDateTimeFormatted, unit, pattern, isBefore)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenCurrentOrThrow(
    targetDateTimeFormatted: String,
    unit: JodaTimeUnit,
    pattern: String = DEFAULT_DATE_PATTERN,
    isBefore: Boolean = true,
): Int {
    val compareDateTime = parseLocalDateTime(targetDateTimeFormatted, LocalType.DATETIME, pattern)
        ?: throw RuntimeException("Incorrect targetDateTimeFormatted: '$targetDateTimeFormatted'")
    return timeBetweenOrThrow(compareDateTime, LocalDateTime.now(), unit, isBefore)
}

@JvmOverloads
fun timeBetween(
    oneDateTimeFormatted: String,
    otherDateTimeFormatted: String,
    oneDateTimePattern: String = DEFAULT_DATE_PATTERN,
    otherDateTimePattern: String = DEFAULT_DATE_PATTERN,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenOrThrow(
        oneDateTimeFormatted,
        otherDateTimeFormatted,
        oneDateTimePattern,
        otherDateTimePattern,
        unit,
        isBefore
    )
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenOrThrow(
    oneDateTimeFormatted: String,
    otherDateTimeFormatted: String,
    oneDateTimePattern: String = DEFAULT_DATE_PATTERN,
    otherDateTimePattern: String = DEFAULT_DATE_PATTERN,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int {
    val oneDateTime = parseLocalDateTime(oneDateTimeFormatted, LocalType.DATETIME, oneDateTimePattern)
        ?: throw RuntimeException("Incorrect oneDateFormatted: '$oneDateTimeFormatted'")
    val otherDateTime = parseLocalDateTime(otherDateTimeFormatted, LocalType.DATETIME, otherDateTimePattern)
        ?: throw RuntimeException("Incorrect otherDateFormatted: '$otherDateTimeFormatted'")
    return timeBetweenOrThrow(oneDateTime, otherDateTime, unit, isBefore)
}

@JvmOverloads
fun timeBetween(
    oneMillis: Long,
    otherMillis: Long,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenOrThrow(oneMillis, otherMillis, unit, isBefore)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

/**
 * С использованием UTC времени с 1970
 */
@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenOrThrow(
    oneMillis: Long,
    otherMillis: Long,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int = timeBetweenOrThrow(LocalDate(oneMillis), LocalDate(otherMillis), unit, isBefore)

@JvmOverloads
fun timeBetween(
    oneDate: BaseLocal,
    otherDate: BaseLocal,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int? = try {
    timeBetweenOrThrow(oneDate, otherDate, unit, isBefore)
} catch (e: RuntimeException) {
    logger.e(e)
    null
}

@Throws(RuntimeException::class)
@JvmOverloads
fun timeBetweenOrThrow(
    oneDate: BaseLocal,
    otherDate: BaseLocal,
    unit: JodaTimeUnit,
    isBefore: Boolean = true,
): Int {
    val first: BaseLocal
    val second: BaseLocal
    val isSecondAfter = otherDate.isAfter(oneDate)
    if (isSecondAfter) {
        first = oneDate
        second = otherDate
    } else {
        first = otherDate
        second = oneDate
    }
    val diff: Int = try {
        when (unit) {
            JodaTimeUnit.YEARS -> Years.yearsBetween(first, second).years
            JodaTimeUnit.MONTHS -> Months.monthsBetween(first, second).months
            JodaTimeUnit.WEEKS -> Weeks.weeksBetween(first, second).weeks
            JodaTimeUnit.DAYS -> Days.daysBetween(first, second).days
            JodaTimeUnit.HOURS -> Hours.hoursBetween(first, second).hours
            JodaTimeUnit.MINUTES -> Minutes.minutesBetween(first, second).minutes
            JodaTimeUnit.SECONDS -> Seconds.secondsBetween(first, second).seconds
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

@JvmOverloads
fun parseLocalDateTime(
    dateTimeFormatted: String?,
    type: LocalType,
    pattern: String,
    formatterConfigFunc: ((DateTimeFormatter) -> Unit)? = null,
): BaseLocal? {
    return try {
        val formatter = DateTimeFormat.forPattern(pattern)
        formatterConfigFunc?.invoke(formatter)
        when (type) {
            LocalType.TIME -> LocalTime.parse(dateTimeFormatted, formatter)
            LocalType.DATE -> LocalDate.parse(dateTimeFormatted, formatter)
            LocalType.DATETIME -> LocalDateTime.parse(dateTimeFormatted, formatter)
        }
    } catch (e: Exception) {
        logger.e(formatException(e, "org.joda.time parse, dateTimeFormatted: '$dateTimeFormatted'"))
        null
    }
}

@JvmOverloads
fun formatLocalDateTime(
    dateTime: BaseLocal?,
    pattern: String,
    formatterConfigFunc: ((DateTimeFormatter) -> Unit)? = null,
): String {
    return try {
        val formatter = DateTimeFormat.forPattern(pattern)
        formatterConfigFunc?.invoke(formatter)
        formatter.print(dateTime)
    } catch (e: Exception) {
        logger.e(formatException(e, "org.joda.time format, dateTime: '$dateTime'"))
        EMPTY_STRING
    }
}


enum class JodaTimeUnit {
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    MONTHS,
    WEEKS,
    YEARS
}

enum class LocalType {
    TIME,
    DATE,
    DATETIME
}
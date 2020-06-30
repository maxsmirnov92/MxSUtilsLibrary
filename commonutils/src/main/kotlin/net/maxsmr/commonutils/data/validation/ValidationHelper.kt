package net.maxsmr.commonutils.data.validation

import net.maxsmr.commonutils.android.livedata.wrappers.LiveDataCanError
import net.maxsmr.commonutils.data.*
import net.maxsmr.commonutils.data.conversion.format.MONTH_YEAR_DATE_DOTTED_PATTERN
import net.maxsmr.commonutils.data.conversion.format.YEAR_MONTH_DAY_DATE_DOTTED_PATTERN
import net.maxsmr.commonutils.data.conversion.toNotNullIntNoThrow
import net.maxsmr.commonutils.data.entity.EmptyValidable
import net.maxsmr.commonutils.data.exception.EmptyFieldException
import net.maxsmr.commonutils.data.exception.NotValidFieldException
import net.maxsmr.commonutils.data.exception.toPairWithNotValidFieldException
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Pair

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("ValidationHelper")

/**
 * Класс-делегат для валидации значений полей ввода
 */
interface ValidationHelper {
    fun <T> validate(value: LiveDataCanError<T>, validationFunc: (T?) -> Throwable?): Boolean

    fun asMonthYear(value: String): Throwable? = asMonthYearWithTime(value).second
    fun asMonthYearWithTime(value: String): Pair<Long?, Throwable?>
    fun asYearMonthDay(value: String) = asYearMonthDayWithTime(value).second
    fun asYearMonthDayWithTime(value: String): Pair<Long?, Throwable?>
    fun <T> asNullable(value: T?): Throwable?
    fun <T : EmptyValidable> asEmpty(value: T?): Throwable?
    fun <T : EmptyValidable> asEmptyList(value: List<T?>): Throwable?
    fun asEmptyText(value: String?): Throwable?
    fun asPhone(value: String): Throwable?
    fun asDecimal(value: BigDecimal?): Throwable?
    fun asEmail(value: String): Throwable?
    fun asBoolean(value: Boolean): Throwable?
}

class ValidationHelperImpl : ValidationHelper {

    private val MIN_YEAR = 1950

    private val yearValidationRule: (DateField, Int) -> Boolean = { field, value ->
        if (field == DateField.YEAR) {
            value >= MIN_YEAR
        } else {
            true
        }
    }

    override fun <T> validate(
            value: LiveDataCanError<T>,
            validationFunc: (T?) -> Throwable?
    ): Boolean = validationFunc.invoke(value.value)
            .also { value.error.setValue(it) }.isNull()

    override fun asMonthYearWithTime(value: String): Pair<Long?, Throwable?> =
            value.validateDateWithMonthYear(NotValidFieldException(), yearValidationRule)
                    .toPairWithNotValidFieldException()

    override fun asYearMonthDayWithTime(value: String): Pair<Long?, Throwable?> =
            value.validateDateWithYearMonthDay(NotValidFieldException(), yearValidationRule)
                    .toPairWithNotValidFieldException()

    override fun <T> asNullable(value: T?): Throwable? =
            value.ifNull { EmptyFieldException() }

    override fun <T : EmptyValidable> asEmpty(value: T?): Throwable? {
        var isEmpty = true
        value?.let { v ->
            if (!v.isEmpty()) {
                isEmpty = false
            }
        }
        return if (isEmpty) EmptyFieldException() else null
    }

    override fun <T : EmptyValidable> asEmptyList(value: List<T?>): Throwable? {
        var result: Throwable? = EmptyFieldException()
        if (value.isNotEmpty()) {
            result = null
            value.forEach {
                result = asEmpty(it)
                if (result != null) {
                    return@forEach
                }
            }
        }
        return result
    }

    override fun asEmptyText(value: String?): Throwable? =
            value.isNullOrBlank().isTrue { EmptyFieldException() }

    override fun asPhone(value: String): Throwable? =
            when {
                value.isBlank() -> EmptyFieldException()
                isPhoneNumberRusValid(value).not() -> NotValidFieldException()
                else -> null
            }

    override fun asDecimal(value: BigDecimal?): Throwable? = value.isNull().isTrue { EmptyFieldException() }

    override fun asEmail(value: String): Throwable? =
            when {
                value.isBlank() -> EmptyFieldException()
                isEmailValid(value).not() -> NotValidFieldException()
                else -> null
            }

    override fun asBoolean(value: Boolean): Throwable? =
            value.isFalse { NotValidFieldException() }
}

/**
 * Преобразовать исходную строку в формате [MONTH_YEAR_DATE_DOTTED_PATTERN]
 * в формат [YEAR_MONTH_DAY_DATE_DOTTED_PATTERN] или, если уже в целевом формате,
 * вернуть её же
 */
fun ValidationHelper.formatToYearMonthDay(source: String): String {
    var result: Pair<Long?, Throwable?> = asYearMonthDayWithTime(source)
    if (result.hasError()) {
        result = asMonthYearWithTime(source)
        result.second.let { exception ->
            if (result.hasError()) {
                throw if (exception !is NotValidFieldException) {
                    NotValidFieldException(cause = exception)
                } else {
                    exception
                }
            }
            return SimpleDateFormat(YEAR_MONTH_DAY_DATE_DOTTED_PATTERN, Locale.getDefault()).format(Date(result.first as Long))
        }
    }
    return source
}

/**
 * Преобразовать исходную строку в формате [YEAR_MONTH_DAY_DATE_DOTTED_PATTERN]
 * в формат [MONTH_YEAR_DATE_DOTTED_PATTERN] или, если уже в целевом формате,
 * вернуть её же
 */
fun ValidationHelper.formatToMonthYear(source: String): String {
    var result: Pair<Long?, Throwable?> = asMonthYearWithTime(source)
    if (result.hasError()) {
        result = asYearMonthDayWithTime(source)
        result.second.let { exception ->
            if (result.hasError()) {
                throw if (exception !is NotValidFieldException) {
                    NotValidFieldException(cause = exception)
                } else {
                    exception
                }
            }
            return SimpleDateFormat(MONTH_YEAR_DATE_DOTTED_PATTERN, Locale.getDefault()).format(Date(result.first as Long))
        }
    }
    return source
}

/**
 * @param shouldReturnSourceIfFailed если не смогли сконвертировать в ожидаемый, то true - исходный, false - [EMPTY_STRING]
 */
fun ValidationHelper.formatToYearMonthDayNoThrow(source: String, shouldReturnSourceIfFailed: Boolean = true): String =
        try {
            formatToYearMonthDay(source)
        } catch (e: NotValidFieldException) {
            if (shouldReturnSourceIfFailed) {
                logger.e("formatToYearMonthDay failed with exception: $e")
                // возвращаем исходную неправильную строку для дальнейшего использования
                source
            } else {
                EMPTY_STRING
            }
        }

/**
 * @param shouldReturnSourceIfFailed  если не смогли сконвертировать в ожидаемый, то true - исходный, false - [EMPTY_STRING]
 */
fun ValidationHelper.formatToMonthYearNoThrow(source: String, shouldReturnSourceIfFailed: Boolean = true): String =
        try {
            formatToMonthYear(source)
        } catch (e: NotValidFieldException) {
            logger.e("formatToMonthYear failed with exception: $e")
            if (shouldReturnSourceIfFailed) {
                // возвращаем исходную неправильную строку для дальнейшего использования
                source
            } else {
                EMPTY_STRING
            }
        }

/**
 * @return пара с временем, если конвертация прошла успешно,
 * и с [Throwable], в противном случае
 */
fun Map<DateField, Int>.toTimeNoThrow(): Pair<Long?, Throwable?> {
    // если все поля прошли валидацию
    with(Calendar.getInstance()) {
        entries.forEach {
            when (it.key) {
                DateField.YEAR -> set(Calendar.YEAR, it.value)
                DateField.MONTH -> set(Calendar.MONTH, if (it.value > 0) it.value - 1 else 0) // в календаре счёт месяцев начинается от нуля
                DateField.DAY_OF_MONTH -> set(Calendar.DAY_OF_MONTH, it.value)
            }
        }
        // сброс в старт месяца, если не выставляли его здесь
        return toTimeNoThrow(!keys.contains(DateField.DAY_OF_MONTH))
    }
}

/*
**
* @return пара с временем, если конвертация прошла успешно,
* и с [Throwable], в противном случае
* @param toStartOfMonth сбросить календарь в начало месяца
* @param toStartOfDay сбросить календарь в начало дня
*/
fun Calendar.toTimeNoThrow(toStartOfMonth: Boolean,
                           toStartOfDay: Boolean = true): Pair<Long?, Throwable?> {
    var time = if (toStartOfMonth) {
        time.toMonthStart()
    } else {
        time
    }
    if (toStartOfDay) {
        time = time.toDayStart()
    }
    return try {
        Pair(time.time, null)
    } catch (e: Throwable) {
        Pair(null, e)
    }
}

/**
 * @return из числа провалидированных [DateField]
 * те, которые [isValid]
 */
fun Map<DateField, Boolean>.filterFieldsByValid(isValid: Boolean): Set<DateField> =
        filter {
            it.value == isValid
        }.map {
            it.key
        }.toSet()

/**
 * Невалидные [DateField] преобразовать в [Throwable]
 */
fun Collection<DateField>.toException(defaultException: Throwable?): Throwable? =
        if (isNotEmpty()) {
            DateValidationException(toSet())
        } else {
            defaultException
        }

/**
 * Провалидировать строку с датой формата [MONTH_YEAR_DATE_DOTTED_PATTERN]
 * @param defaultException на случай, когда нет конкретных невалидных филдов
 * @param additionalRule дополнительное правило, которое должно быть применено к полю даты типа [DateField]
 */
private fun String.validateDateWithMonthYear(
        defaultException: Throwable?,
        additionalRule: ((DateField, Int) -> Boolean)? = null
): Pair<Long?, Throwable?> {
    var nonValidFields = setOf<DateField>()
    if (isNotEmpty()) {
        val parts = split(".")
        if (parts.size == 2) {

            val year = parts[1].toNotNullIntNoThrow()
            val month = parts[0].toNotNullIntNoThrow()

            mapOf(
                    DateField.MONTH to
                            (DateField.MONTH.validate(month) && additionalRule?.invoke(DateField.MONTH, month) ?: true),
                    DateField.YEAR to
                            (DateField.YEAR.validate(year) && additionalRule?.invoke(DateField.YEAR, year) ?: true)
            ).apply {
                with(filterFieldsByValid(false)) {
                    nonValidFields = this
                    if (isEmpty()) {
                        // все поля прошли валидацию, создаём календарь
                        return mapOf(
                                DateField.YEAR to year,
                                DateField.MONTH to month
                        ).toTimeNoThrow()
                    }
                }
            }
        }
    }
    return Pair(null, nonValidFields.toException(defaultException))
}

/**
 * Провалидировать строку с датой формата [YEAR_MONTH_DAY_DATE_DOTTED_PATTERN]
 * @param defaultException на случай, когда нет конкретных невалидных филдов
 * @param additionalRule дополнительное правило, которое должно быть применено к полю даты типа [DateField]
 */
private fun String.validateDateWithYearMonthDay(
        defaultException: Throwable?,
        additionalRule: ((DateField, Int) -> Boolean)? = null
): Pair<Long?, Throwable?> {
    var nonValidFields = setOf<DateField>()
    if (isNotEmpty()) {
        val parts = split(".")
        if (parts.size == 3) {

            val year = parts[0].toNotNullIntNoThrow()
            val month = parts[1].toNotNullIntNoThrow()
            val dayOfMonth = parts[2].toNotNullIntNoThrow()

            mapOf(
                    DateField.DAY_OF_MONTH to
                            (DateField.DAY_OF_MONTH.validate(dayOfMonth) && additionalRule?.invoke(DateField.DAY_OF_MONTH, dayOfMonth) ?: true),
                    DateField.MONTH to
                            (DateField.MONTH.validate(month) && additionalRule?.invoke(DateField.MONTH, month) ?: true),
                    DateField.YEAR to
                            (DateField.YEAR.validate(year) && additionalRule?.invoke(DateField.YEAR, year) ?: true)
            ).apply {
                with(filterFieldsByValid(false)) {
                    nonValidFields = this
                    if (isEmpty()) {
                        // все поля прошли валидацию, создаём календарь
                        return mapOf(
                                DateField.YEAR to year,
                                DateField.MONTH to month,
                                DateField.DAY_OF_MONTH to dayOfMonth
                        ).toTimeNoThrow()
                    }
                }
            }
        }
    }
    return Pair(null, nonValidFields.toException(defaultException))
}

private fun Pair<Long?, Throwable?>.hasError() = first == null || second != null

/**
 * Поле даты с возможностью валидации
 */
enum class DateField {

    DAY_OF_MONTH, MONTH, YEAR;

    fun validate(value: Int): Boolean = when (this) {
        DAY_OF_MONTH -> value in 1..31
        MONTH -> value in 1..12
        YEAR -> value > 0
    }
}

/**
 * @param nonValidFields список [DateField],
 * не прошедших валидацию
 */
class DateValidationException(
        val nonValidFields: Set<DateField>
) : Throwable(nonValidFields.toString())
package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.BigDecimal.ROUND_HALF_UP
import java.math.BigDecimal.ZERO
import kotlin.math.abs

fun equal(first: BigDecimal, second: BigDecimal?): Boolean {
    second ?: return false
    return first.compareTo(second) == 0 // "=="
}

fun isZero(value: BigDecimal): Boolean {
    return equal(value, ZERO)
}

fun isNotZero(value: BigDecimal): Boolean {
    return isZero(value).not()
}

fun isNotZeroOrNull(value: BigDecimal?): Boolean {
    return value != null && isZero(value).not()
}

fun isLess(first: BigDecimal, second: BigDecimal?): Boolean {
    second ?: return false
    return first.compareTo(second) < 0 // "<"
}

fun isGreater(first: BigDecimal, second: BigDecimal?): Boolean {
    second ?: return false
    return first.compareTo(second) > 0 // ">"
}

fun isLessOrEquals(first: BigDecimal, second: BigDecimal?): Boolean {
    second ?: return false
    return first.compareTo(second) <= 0 // "<="
}

fun isGreaterOrEquals(first: BigDecimal, second: BigDecimal?): Boolean {
    second ?: return false
    return first.compareTo(second) >= 0 // ">="
}

fun fraction(value: Number?): BigDecimal =
        value?.let {
            val source = it.toDouble()
            abs(source - source.toLong()).toBigDecimal()
        } ?: ZERO

fun removeFractionIfZero(value: Number?): BigDecimal =
        value?.let {
            with(fraction(value)) {
                if (isZero(this)) {
                    it.toDouble().toLong().toBigDecimal()
                } else {
                    if (it is BigDecimal) {
                        it
                    } else {
                        it.toDouble().toBigDecimal()
                    }
                }
            }
        } ?: ZERO


/**
 * Высчитывает процент от [total]
 */
fun computePercent(value: BigDecimal, total: BigDecimal) =
        if (isZero(total))
            total
        else
            value.divide(total, 4, ROUND_HALF_UP).multiply(BigDecimal(100))

/**
 * @return null или исходный в остальных случаях
 */
fun toValueOrNull(value: BigDecimal?): BigDecimal? = if (value != null) correctZero(value) else null

/**
 * @return [BigDecimal.ZERO] если null или 0.0
 * и исходный в остальных случаях
 */
fun toValueOrZero(value: BigDecimal?): BigDecimal = if (value != null) correctZero(value) else ZERO

/**
 * @return [BigDecimal.ZERO] если 0.0
 * и исходный в остальных случаях
 * (чтобы далее сравнивать по isZero, не заботясь о 0.0)
 */
fun correctZero(value: BigDecimal): BigDecimal = if (value == 0.0.toBigDecimal()) ZERO else value
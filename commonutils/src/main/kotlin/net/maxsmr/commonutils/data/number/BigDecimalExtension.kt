package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.BigDecimal.ROUND_HALF_UP
import java.math.BigDecimal.ZERO
import kotlin.math.abs

fun BigDecimal.equal(another: BigDecimal?): Boolean {
    another ?: return false
    return compareTo(another) == 0 // "=="
}

fun BigDecimal.isZero(): Boolean {
    return equal(ZERO)
}

fun BigDecimal?.isZeroOrNull() =
        this == null || isZero()

fun BigDecimal.isNotZero(): Boolean {
    return isZero().not()
}

fun BigDecimal?.isNotZeroOrNull(): Boolean {
    return this != null && isNotZero()
}

fun BigDecimal.isLess(another: BigDecimal?): Boolean {
    another ?: return false
    return this < another
}

fun BigDecimal.isGreater(second: BigDecimal?): Boolean {
    second ?: return false
    return this > second
}

fun BigDecimal.isLessOrEquals(another: BigDecimal?): Boolean {
    another ?: return false
    return this <= another
}

fun BigDecimal.isGreaterOrEquals(another: BigDecimal?): Boolean {
    another ?: return false
    return this >= another
}

fun Number?.fraction(): BigDecimal =
        this?.let {
            val source = it.toDouble()
            abs(source - source.toLong()).toBigDecimal()
        } ?: ZERO

fun Number?.removeFractionIfZero(): BigDecimal =
        this?.let {
            with(fraction()) {
                if (isZero()) {
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
fun BigDecimal.computePercent(total: BigDecimal) =
        if (isZero()) {
            total
        } else {
            divide(total, 4, ROUND_HALF_UP).multiply(BigDecimal(100))
        }

/**
 * @return null или исходный в остальных случаях
 */
fun BigDecimal?.toValueOrNull(): BigDecimal? = this?.correctZero()

/**
 * @return [BigDecimal.ZERO] если null или 0.0
 * и исходный в остальных случаях
 */
fun BigDecimal?.toValueOrZero(): BigDecimal = this?.correctZero() ?: ZERO

/**
 * @return [BigDecimal.ZERO] если 0.0
 * и исходный в остальных случаях
 * (чтобы далее сравнивать по isZero, не заботясь о 0.0)
 */
fun BigDecimal.correctZero(): BigDecimal = if (equal(0.0.toBigDecimal())) ZERO else this
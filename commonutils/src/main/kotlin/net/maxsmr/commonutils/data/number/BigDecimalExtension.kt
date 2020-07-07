package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.BigDecimal.ROUND_HALF_UP
import java.math.BigDecimal.ZERO
import kotlin.math.pow

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

fun BigDecimal?.fraction(): BigDecimal =
        this?.let {
            val source = it
            source - source.toLong().toBigDecimal()
        } ?: ZERO


fun BigDecimal?.removeFractionIfZero(): BigDecimal =
        this?.let {
            with(fraction()) {
                if (isZero()) {
                    it.toDouble().toLong().toBigDecimal()
                } else {
                    it
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

fun BigDecimal?.rublesToKopecksSimple(): Long = mergeFractionSimple(2)

fun BigDecimal?.mergeFractionSimple(count: Int): Long {
    if (this == null) return 0
    val grade = 10.0.toBigDecimal().pow(count)
    return (this * grade).toLong()
}

fun BigDecimal?.rublesToKopecks(): Long = mergeFraction(2)

fun BigDecimal?.mergeFraction(count: Int): Long {
    require(count > 0) { "count must be at least 1" }
    if (this == null) return 0
    val source = this
            .setScale(count) // оставляем n знаков после запятой
    var fraction = source.fraction() // отбрасываем целую часть
    val grade = 10.0.pow(count)
    fraction = fraction.multiply(grade.toBigDecimal()) // оставшаяся дробная часть == копейки
    var kopecks = this.toLong() * grade.toLong()
    kopecks += fraction.toLong()
    return kopecks
}
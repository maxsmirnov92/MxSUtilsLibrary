package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.safeCompare(another: Double) =
        BigDecimal(this).compareTo(BigDecimal(another))

fun Double.isEqual(another: Double) =
        safeCompare(another) == 0

fun Double.isZero() =
        safeCompare(.0) == 0

fun Double?.isZeroOrNull() =
        this == null || isZero()

fun Double.isNotZero() =
        !isZero()

fun Double?.isNotZeroOrNull() =
        this != null && isNotZero()

fun Double.isLess(another: Double?) =
        another != null && safeCompare(another) < 0

fun Double.isGreater(another: Double?) =
        another != null && this.safeCompare(another) > 0

fun Double.isLessOrEquals(another: Double?): Boolean =
        another != null && safeCompare(another) <= 0

fun Double.isGreaterOrEquals(another: Double?): Boolean =
        another != null && safeCompare(another) >= 0

fun Double?.fraction(): Double =
        this?.let {
            val source = it
            abs(source - source.toLong())
        } ?: 0.0

fun Double?.removeFractionIfZero(): BigDecimal =
        this?.let {
            with(fraction()) {
                if (isZero()) {
                    it.toDouble().toLong().toBigDecimal()
                } else {
                    it.toBigDecimal()
                }
            }
        } ?: ZERO

fun Double.divide(divider: Double): Double {
    if (divider.isZero()) {
        return .0
    }
    return BigDecimal(this).divide(BigDecimal(divider), 10, RoundingMode.HALF_UP).toDouble()
}

fun Double?.toBigDecimalRoundScale(
        scale: Int,
        roundingMode: RoundingMode = RoundingMode.HALF_UP
): BigDecimal {
    return if (this != null) {
        BigDecimal(this).setScale(scale, roundingMode)
    } else {
        ZERO
    }
}

fun Double.multiply(another: Double) =
        BigDecimal(this).multiply(BigDecimal(another)).toDouble()

fun Double?.round(precision: Int): Double {
    require(precision >= 0) { "Incorrect precision: $precision" }
    if (this == null) return 0.0
    var delimiter = 1.0
    for (i in 0 until precision) {
        delimiter *= 10.0
    }
    return (this * delimiter).roundToInt().toDouble() / delimiter
}

fun Double?.rublesToKopecksSimple(): Long = mergeFractionSimple(2)

fun Double?.mergeFractionSimple(count: Int): Long {
    if (this == null) return 0
    val grade = 10.0.pow(count)
    return (this * grade).toLong()
}

fun Double?.rublesToKopecks(): Long = mergeFraction(2)

fun Double?.mergeFraction(count: Int): Long {
    require(count > 0) { "count must be at least 1" }
    if (this == null) return 0
    val source = this
            .round(count) // оставляем n знаков после запятой
            .toBigDecimal()
    var fraction = source.fraction() // отбрасываем целую часть
    val grade = 10.0.pow(count)
    fraction = fraction.multiply(grade.toBigDecimal()) // оставшаяся дробная часть == копейки
    var kopecks = this.toLong() * grade.toLong()
    kopecks += fraction.toLong()
    return kopecks
}
package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.RoundingMode
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
        BigDecimal.ZERO
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
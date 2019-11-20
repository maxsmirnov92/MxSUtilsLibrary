package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.safeCompare(value: Double) =
        BigDecimal(this).compareTo(BigDecimal(value))

fun Double.isEqual(value: Double) =
        this.safeCompare(value) == 0

fun Double.isZero() =
        this.safeCompare(.0) == 0

fun Double.isNotZero() =
        this.safeCompare(.0) != 0

fun Double.divide(divider: Double): Double {
    if (divider.isZero()) {
        return .0
    }
    return BigDecimal(this).divide(BigDecimal(divider), 10, RoundingMode.HALF_UP).toDouble()
}

fun Double.multiply(value: Double) =
        BigDecimal(this).multiply(BigDecimal(value)).toDouble()

fun Double.isBiggerThan(value: Double) =
        this.safeCompare(value) == 1


fun Double.isSmallerThan(value: Double) =
        this.safeCompare(value) == -1

fun Double.isBiggerOrEqualThan(value: Double): Boolean {
    val compareResult = this.safeCompare(value)
    return compareResult == 1 || compareResult == 0
}

fun Double.isSmallerOrEqualThan(value: Double): Boolean {
    val compareResult = this.safeCompare(value)
    return compareResult == -1 || compareResult == 0
}

fun Double.roundTo2DecimalPlaces() =
        BigDecimal(this).setScale(2, BigDecimal.ROUND_DOWN).toDouble()

fun Double?.toBigDecimalRoundScale2(): BigDecimal {
    return this.toBigDecimalRoundScale(2)
}

fun Double?.toBigDecimalRoundScale3(): BigDecimal {
    return this.toBigDecimalRoundScale(3)
}

fun Double?.toBigDecimalRoundScale(scale: Int): BigDecimal {
    return if (this != null) {
        BigDecimal(this).setScale(scale, BigDecimal.ROUND_HALF_UP)
    } else {
        BigDecimal.ZERO
    }
}
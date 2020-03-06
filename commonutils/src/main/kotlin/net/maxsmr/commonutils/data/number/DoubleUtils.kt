package net.maxsmr.commonutils.data.number

import java.math.BigDecimal
import java.math.RoundingMode

fun safeCompare(one: Double, another: Double) =
        BigDecimal(one).compareTo(BigDecimal(another))

fun equal(one: Double, another: Double) =
        safeCompare(one, another) == 0

fun isZero(value: Double) =
        safeCompare(value, .0) == 0

fun isZeroOrNull(value: Double?) =
        value == null || isZero(value)

fun isNotZero(value: Double) =
        !isZero(value)

fun isNotZeroOrNull(value: Double?) =
        value != null && isNotZero(value)

fun isLess(one: Double, another: Double) =
        safeCompare(one, another) == -1

fun isGreater(one: Double, another: Double) =
        safeCompare(one, another) == 1

fun isLessOrEqual(one: Double, another: Double): Boolean {
    val compareResult = safeCompare(one, another)
    return compareResult <= 0
}

fun isGreaterOrEqual(one: Double, another: Double): Boolean {
    val compareResult = safeCompare(one, another)
    return compareResult >= 0
}

fun divide(value: Double, divider: Double): Double {
    if (isZero(divider)) {
        return .0
    }
    return BigDecimal(value).divide(BigDecimal(divider), 10, RoundingMode.HALF_UP).toDouble()
}

fun multiply(one: Double, another: Double) =
        BigDecimal(one).multiply(BigDecimal(another)).toDouble()

fun toBigDecimalRoundScale2(value: Double?): BigDecimal =
        toBigDecimalRoundScale(value, 2)

fun toBigDecimalRoundScale3(value: Double?): BigDecimal {
    return toBigDecimalRoundScale(value, 3)
}

fun toBigDecimalRoundScale(
        value: Double?,
        scale: Int,
        roundingMode: RoundingMode = RoundingMode.HALF_UP
): BigDecimal {
    return if (value != null) {
        BigDecimal(value).setScale(scale, roundingMode)
    } else {
        BigDecimal.ZERO
    }
}
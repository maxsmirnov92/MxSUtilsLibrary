package net.maxsmr.commonutils.number

import java.lang.StringBuilder
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode
import kotlin.math.*

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
                    it.toLong().toBigDecimal()
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

/**
 * Округление по правилам математики с заданной точностью
 */
fun Double.round(precision: Int): Double {
    if (precision < 0) return this
    if (precision == 0) return toInt().toDouble()
    val decimalPart = this.toString().split(".")[1]
    if (decimalPart == "0" || decimalPart.count() <= precision) return this
    val decimalPow = 10.0.pow(decimalPart.count().toDouble() - 1)
    return ((this * decimalPow).roundToInt().toDouble() / decimalPow).round(precision)
}

/**
 * Округляет число с плавающей точкой до [precision] знаков после запятой по правилам округления
 * (с использованием String.format)
 */
fun Double.roundFormatted(precision: Int): Double {
    return "%.${precision}f".format(this).replace(',','.').toDouble()
}

/**
 * Округляет число с плавающей точкой до ближайшего целого
 */
fun Double.roundInt(): Int {
    return ceil(this).toInt()
}

fun Double?.mergeFraction(count: Int): Long {
    if (this == null || count <= 0) return 0
    val grade = 10.0.pow(count)
    return (this * grade).roundToLong()
}

fun Long?.splitWithFraction(count: Int): Double {
    if (this == null || count <= 0) return 0.0
    val grade = 10.0.pow(count).toLong()
    // деление должно быть целочисленным!
    val intPart = this / grade
    val fractionPart = StringBuilder((this % grade).toString())
    if (fractionPart.length < count) {
        for (i in 0 until count - fractionPart.length) {
            fractionPart.insert(0, '0')
        }
    }
    return "$intPart.$fractionPart".toDouble()
}

@JvmOverloads
fun Long?.splitWithFractionByRound(count: Int, precision: Int = count): Double {
    if (this == null) return 0.0
    val grade = 10.0.pow(count)
    return (this.toDouble() / grade).roundFormatted(precision)
}

fun Double?.rublesToKopecks(): Long = mergeFraction(2)

fun Long?.kopecksToRubles(): Double = splitWithFraction(2)

fun Long?.kopecksToRublesByRound(): Double = splitWithFractionByRound(2)
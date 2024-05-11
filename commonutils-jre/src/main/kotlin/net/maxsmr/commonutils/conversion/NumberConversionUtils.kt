package net.maxsmr.commonutils.conversion

import java.math.BigDecimal

@JvmOverloads
fun CharSequence?.toByteNotNull(
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Byte = this.toByteOrNull(radix, exceptionAction) ?: 0

@JvmOverloads
fun CharSequence?.toByteOrNull(
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Byte? = toNumberOrThrow(Byte::class.java, radix, exceptionAction)

@JvmOverloads
fun CharSequence?.toIntNotNull(
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int = this.toIntOrNull(radix, exceptionAction) ?: 0

@JvmOverloads
fun CharSequence?.toIntOrNull(
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int? = toNumberOrThrow(Int::class.java, radix, exceptionAction)

@JvmOverloads
fun CharSequence?.toLongNotNull(
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Long = toLongOrNull(radix, exceptionAction) ?: 0L

@JvmOverloads
fun CharSequence?.toLongOrNull(
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Long? = toNumberOrThrow(Long::class.java, radix, exceptionAction)

@JvmOverloads
fun CharSequence?.toFloatNotNull(
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Float = toFloatOrNull(exceptionAction) ?: 0f

@JvmOverloads
fun CharSequence?.toFloatOrNull(
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Float? = toNumberOrThrow(Float::class.java, 10, exceptionAction)

@JvmOverloads
fun CharSequence?.toDoubleNotNull(
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double = toDoubleOrNull(exceptionAction) ?: 0.0

@JvmOverloads
fun CharSequence?.toDoubleOrNull(
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double? = toNumberOrThrow(Double::class.java, 10, exceptionAction)

fun Number.toBigDecimal(): BigDecimal {
    return if (this is BigDecimal) {
        this
    } else {
        BigDecimal(this.toDouble())
    }
}

/**
 * @throws IllegalArgumentException if [numberType] is incorrect
 */
@Suppress("UNCHECKED_CAST")
@Throws(IllegalArgumentException::class)
fun <N : Number?> CharSequence?.toNumberOrThrow(
    numberType: Class<N>,
    radix: Int = 10,
    exceptionAction: ((NumberFormatException) -> Unit)? = null
): N? {
    if (this == null) {
        return null
    }
    val numberString = this.toString()
    when {
        numberType.isAssignableFrom(Byte::class.java) -> {
            return try {
                numberString.toByte(radix) as N
            } catch (e: NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }

        numberType.isAssignableFrom(Int::class.java) -> {
            return try {
                numberString.toInt(radix) as N
            } catch (e: NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }

        numberType.isAssignableFrom(Long::class.java) -> {
            return try {
                numberString.toLong(radix) as N
            } catch (e: NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }

        numberType.isAssignableFrom(Float::class.java) -> {
            return try {
                numberString.toFloat() as N
            } catch (e: NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }

        numberType.isAssignableFrom(Double::class.java) -> {
            return try {
                numberString.toDouble() as N
            } catch (e: NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }

        else -> throw IllegalArgumentException("Incorrect number class: $numberType")
    }
}

fun Long.toIntSafe(): Int {
    require(!(this < Int.MIN_VALUE || this > Int.MAX_VALUE)) { "$this cannot be cast to int without changing its value." }
    return this.toInt()
}

fun mergeDigits(numbers: Collection<Number?>?): Double {
    var result = 0.0
    var currentMultiplier = 1
    if (numbers != null) {
        for (n in numbers) {
            if (n != null) {
                result += n.toDouble() * currentMultiplier.toDouble()
                currentMultiplier *= 10
            }
        }
    }
    return result
}

fun splitNumberToDigits(number: Number?): List<Int>? {
    val result = mutableListOf<Int>()
    if (number != null) {
        var n: Int = number.toInt()
        while (n > 0) {
            result.add(0, n % 10)
            n /= 10
        }
    }
    return result
}
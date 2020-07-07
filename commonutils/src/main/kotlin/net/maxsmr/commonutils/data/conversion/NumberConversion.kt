package net.maxsmr.commonutils.data.conversion

@JvmOverloads
fun CharSequence?.toNotNullByteNoThrow(
        radix: Int = 10,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Byte = this.toByteNoThrow(radix, exceptionAction) ?: 0

@JvmOverloads
fun CharSequence?.toByteNoThrow(
        radix: Int = 10,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Byte? = toNumber(Byte::class.java, radix, exceptionAction)

@JvmOverloads
fun CharSequence?.toNotNullIntNoThrow(
        radix: Int = 10,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int = this.toIntNoThrow(radix, exceptionAction) ?: 0

@JvmOverloads
fun CharSequence?.toIntNoThrow(
        radix: Int = 10,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int? = toNumber(Int::class.java, radix, exceptionAction)

@JvmOverloads
fun CharSequence?.toNotNullLongNoThrow(
        radix: Int = 10,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Long = toLongNoThrow(radix, exceptionAction) ?: 0L

@JvmOverloads
fun CharSequence?.toLongNoThrow(
        radix: Int = 10,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Long? = toNumber(Long::class.java, radix, exceptionAction)

@JvmOverloads
fun CharSequence?.toFloatNotNullNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Float = toFloatNoThrow(exceptionAction) ?: 0f

@JvmOverloads
fun CharSequence?.toFloatNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Float? = toNumber(Float::class.java, 10, exceptionAction)

@JvmOverloads
fun CharSequence?.toDoubleNotNullNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double = toDoubleNoThrow(exceptionAction) ?: 0.0

@JvmOverloads
fun CharSequence?.toDoubleNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double? = toNumber(Double::class.java, 10, exceptionAction)

@Suppress("UNCHECKED_CAST")
fun <N : Number?> CharSequence?.toNumber(
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
            } catch (e: java.lang.NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }
        numberType.isAssignableFrom(Int::class.java) -> {
            return try {
                numberString.toInt(radix) as N
            } catch (e: java.lang.NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }
        numberType.isAssignableFrom(Long::class.java) -> {
            return try {
                numberString.toLong(radix) as N
            } catch (e: java.lang.NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }
        numberType.isAssignableFrom(Float::class.java) -> {
            return try {
                numberString.toFloat() as N
            } catch (e: java.lang.NumberFormatException) {
                exceptionAction?.invoke(e)
                null
            }
        }
        numberType.isAssignableFrom(Double::class.java) -> {
            return try {
                numberString.toDouble() as N
            } catch (e: java.lang.NumberFormatException) {
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
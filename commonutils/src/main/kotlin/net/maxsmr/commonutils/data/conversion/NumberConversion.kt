package net.maxsmr.commonutils.data.conversion

@JvmOverloads
fun CharSequence?.toDoubleNotNullNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double = toDoubleNoThrow(exceptionAction) ?: 0.0

@JvmOverloads
fun CharSequence?.toDoubleNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double? {
    var result: Double? = null
    this?.let {
        try {
            result = it.toString().toDouble()
        } catch (e: NumberFormatException) {
            exceptionAction?.invoke(e)
        }
    }
    return result
}

@JvmOverloads
fun CharSequence?.toNotNullIntNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int = this.toIntNoThrow( exceptionAction) ?: 0

@JvmOverloads
fun CharSequence?.toIntNoThrow(
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int? {
    var result: Int? = null
    this?.let {
        try {
            result = it.toString().toInt()
        } catch (e: NumberFormatException) {
            exceptionAction?.invoke(e)
        }
    }
    return result
}

@Suppress("UNCHECKED_CAST")
fun <N : Number?> String.toNumber(numberType: Class<N>): N? {
    if (numberType.isAssignableFrom(Byte::class.java)) {
        return try {
            toByte() as N
        } catch (e: java.lang.NumberFormatException) {
            null
        }
    } else if (numberType.isAssignableFrom(Int::class.java)) {
        return try {
            toInt() as N
        } catch (e: java.lang.NumberFormatException) {
            null
        }
    } else if (numberType.isAssignableFrom(Long::class.java)) {
        return try {
            toLong() as N
        } catch (e: java.lang.NumberFormatException) {
            null
        }
    } else if (numberType.isAssignableFrom(Float::class.java)) {
        return try {
            toFloat() as N
        } catch (e: java.lang.NumberFormatException) {
            null
        }
    } else if (numberType.isAssignableFrom(Double::class.java)) {
        return try {
            toDouble() as N
        } catch (e: java.lang.NumberFormatException) {
            null
        }
    }
    throw IllegalArgumentException("incorrect number class: $numberType")
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
package net.maxsmr.commonutils.data.number

@JvmOverloads
fun toDoubleNoThrow(
        value: String?,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double? {
    var result: Double? = null
    value?.let {
        try {
            result = it.toDouble()
        } catch (e: NumberFormatException) {
            exceptionAction?.invoke(e)
        }
    }
    return result
}

@JvmOverloads
fun toDoubleNotNullNoThrow(
        value: String?,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Double = toDoubleNoThrow(value, exceptionAction) ?: 0.0

@JvmOverloads
fun toIntNoThrow(
        value: String?,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int? {
    var result: Int? = null
    value?.let {
        try {
            result = it.toInt()
        } catch (e: NumberFormatException) {
            exceptionAction?.invoke(e)
        }
    }
    return result
}

@JvmOverloads
fun toNotNullIntNoThrow(
        value: String?,
        exceptionAction: ((NumberFormatException) -> Unit)? = null
): Int = toIntNoThrow(value, exceptionAction) ?: 0

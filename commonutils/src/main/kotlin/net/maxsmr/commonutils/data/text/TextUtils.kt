package net.maxsmr.commonutils.data.text

import net.maxsmr.commonutils.data.CompareCondition
import net.maxsmr.commonutils.data.Predicate
import net.maxsmr.commonutils.data.conversion.toDoubleOrNull
import net.maxsmr.commonutils.data.number.isZeroOrNull
import java.util.*

// region: Copied from TextUtils
fun join(delimiter: CharSequence, tokens: Array<Any?>): String =
        join(delimiter, tokens.toList())

fun join(delimiter: CharSequence, tokens: Iterable<*>): String {
    val it = tokens.iterator()
    if (!it.hasNext()) {
        return EMPTY_STRING
    }
    val sb = StringBuilder()
    sb.append(it.next())
    while (it.hasNext()) {
        sb.append(delimiter)
        sb.append(it.next())
    }
    return sb.toString()
}
fun split(text: String, expression: String): Array<String> {
    return if (text.isEmpty()) {
        arrayOf()
    } else {
        text.split(expression).dropLastWhile { it.isEmpty() }.toTypedArray()
    }
}
// endregion

@JvmOverloads
fun isEmpty(s: CharSequence?, shouldCheckNullString: Boolean = false): Boolean =
        s == null || s == EMPTY_STRING || shouldCheckNullString && "null".equals(s.toString(), ignoreCase = true)

fun isZeroOrNull(value: CharSequence?) = value.toDoubleOrNull().isZeroOrNull()

fun isNotZeroOrNull(value: CharSequence?) = !isZeroOrNull(value)

fun getStubValue(value: String?, stringsProvider: () -> String): String? =
        getStubValueWithAppend(value, null, stringsProvider)

fun getStubValue(value: Int, stringsProvider: () -> String): String? =
        getStubValueWithAppend(value, null, stringsProvider)

fun getStubValueWithAppend(value: Int, appendWhat: String?, stringsProvider: () -> String): String? =
        getStubValueWithAppend(if (value != 0) value.toString() else null, appendWhat, stringsProvider)

fun getStubValueWithAppend(value: String?, appendWhat: String?, stringsProvider: () -> String): String? =
        if (!isEmpty(value)) if (!isEmpty(appendWhat)) "$value $appendWhat" else value else stringsProvider.invoke()

fun changeCaseFirstLatter(s: CharSequence?, upper: Boolean): String {
    var result = EMPTY_STRING
    if (s != null && !isEmpty(s)) {
        result = s.toString()
        result = if (s.length == 1) {
            if (upper) result.toUpperCase(Locale.getDefault()) else result.toLowerCase(Locale.getDefault())
        } else {
            ((if (upper) result.substring(0, 1).toUpperCase(Locale.getDefault()) else result.substring(0, 1).toLowerCase(Locale.getDefault()))
                    + result.substring(1))
        }
    }
    return result
}

fun insertAt(target: CharSequence, index: Int, what: CharSequence): String {
    require(!(index < 0 || index >= target.length)) { "incorrect index: $index" }
    return target.toString().substring(0, index) + what + target.toString().substring(index, target.length)
}

fun indexOf(s: CharSequence, c: Char): Int {
    for (index in 0 until s.length) {
        if (s[index] == c) {
            return index
        }
    }
    return -1
}

fun replaceSubstrings(
        text: String?,
        characters: Set<String?>?,
        replacement: String = EMPTY_STRING
): String? {
    var result = text
    if (characters != null)
        for (c in characters) {
            if (c != null) {
                if (result != null && !isEmpty(result)) {
                    result = result.replace(c, replacement)
                }
            }
        }
    return result
}

fun replaceRangeOrThrow(s: CharSequence, start: Int, end: Int, replacement: CharSequence): String {
    checkArgs(s, start, end)
    val sb = StringBuilder(s)
    sb.replace(start, end, replacement.toString())
    return sb.toString()
}

fun replaceRange(s: CharSequence, start: Int, end: Int, replacement: CharSequence): CharSequence =
        try {
            replaceRangeOrThrow(s, start, end, replacement)
        } catch (e: IllegalArgumentException) {
            s
        }

fun removeRangeOrThrow(s: CharSequence, start: Int, end: Int): String {
    checkArgs(s, start, end)
    val sb = StringBuilder(s)
    sb.delete(start, end)
    return sb.toString()
}

fun removeRange(s: CharSequence, start: Int, end: Int): CharSequence =
        try {
            removeRangeOrThrow(s, start, end)
        } catch (e: IllegalArgumentException) {
            s
        }

fun removeSubstrings(
        text: String?,
        characters: Set<String?>?
) = replaceSubstrings(text, characters, EMPTY_STRING)

fun removeNonDigits(s: CharSequence): CharSequence {
    val format = StringBuilder(s)
    var i = 0
    while (i < format.length) {
        if (!Character.isDigit(format[i])) format.deleteCharAt(i) else i++
    }
    return format
}

fun removeCharAt(s: CharSequence, index: Int): String {
    if (index < 0 || index >= s.length) throw StringIndexOutOfBoundsException("incorrect index: $index")
    val sb = StringBuilder(s)
    sb.deleteCharAt(index)
    return sb.toString()
}


fun removeChars(
        text: CharSequence,
        isFromStart: Boolean,
        byChar: Char,
        condition: CompareCondition = CompareCondition.LESS_OR_EQUAL,
        ignoreCase: Boolean = true,
        andRule: Boolean = false,
        excludeByConditions: Boolean = true
): CharSequence = removeChars(text, isFromStart, mapOf(Pair(byChar, CharConditionInfo(condition, ignoreCase))), andRule, excludeByConditions)

/**
 * Убирает указанные символы с начала или конца строки
 * (в отличие от trim не останавливается на первом непрошедшем по данным условиям)
 * @param andRule true - применяется ко всем условиям; false - применяется, если сработало хотя бы одно
 * @param conditions условия исключения или включения указанных символов
 */
fun removeChars(
        text: CharSequence,
        isFromStart: Boolean,
        conditions: Map<Char, CharConditionInfo>,
        andRule: Boolean = false,
        excludeByConditions: Boolean = true
): CharSequence =
        removeChars(text, isFromStart, excludeByConditions) {
            isTrue(conditions, andRule, it)
        }

fun removeChars(
        text: CharSequence,
        isFromStart: Boolean,
        excludeByConditions: Boolean = true,
        predicate: (Char) -> Boolean
): CharSequence {
    val result = StringBuilder()
    var wasAppended = false
    if (isFromStart) {
        for (current in text) {
            val isTrue = predicate(current)
            if (wasAppended || if (excludeByConditions) !isTrue else isTrue) {
                result.append(current)
                wasAppended = true
            }
        }
    } else {
        for (i in text.length - 1 downTo 0) {
            val current = text[i]
            val isTrue = predicate(current)
            if (wasAppended || if (excludeByConditions) !isTrue else isTrue) {
                result.append(current)
                wasAppended = true
            }
        }
        result.reverse()
    }
    return result.toString()
}

fun trimWithCondition(
        text: CharSequence,
        fromStart: Boolean,
        byChar: Char,
        condition: CompareCondition = CompareCondition.LESS_OR_EQUAL,
        ignoreCase: Boolean = true,
        andRule: Boolean = false,
        excludeByConditions: Boolean = true
): CharSequence =
        trimWithCondition(text, fromStart, mapOf(Pair(byChar, CharConditionInfo(condition, ignoreCase))), andRule, excludeByConditions)

fun trimWithCondition(
        text: CharSequence,
        fromStart: Boolean,
        conditions: Map<Char, CharConditionInfo>,
        andRule: Boolean = false,
        excludeByConditions: Boolean = true
): CharSequence =
        trim(text, fromStart, excludeByConditions) {
            isTrue(conditions, andRule, it)
        }

/**
 * Returns a sub sequence of this char sequence having leading/trailing characters matching the [predicate] removed.
 */
fun trim(
        text: CharSequence,
        fromStart: Boolean,
        excludeByPredicate: Boolean = true,
        predicate: (Char) -> Boolean
): CharSequence {
    for (index in if (fromStart) text.indices else text.indices.reversed()) {
        val ch = text[index]
        val isTrue = predicate(ch)
        if (if (excludeByPredicate) !isTrue else isTrue) {
            return if (fromStart) {
                text.subSequence(index, text.length)
            } else {
                text.subSequence(0, index + 1)
            }
        }
    }

    return EMPTY_STRING
}

private fun isTrue(
        conditions: Map<Char, CharConditionInfo>,
        andRule: Boolean,
        currentCh: Char
): Boolean {
    return if (andRule) {
        if (conditions.isEmpty()) {
            return false
        } else {
            !Predicate.Methods.contains(conditions.entries) { entry ->
                !entry.value.condition.apply(entry.key, currentCh, entry.value.ignoreCase)
            }
        }
    } else {
        Predicate.Methods.contains(conditions.entries) { entry ->
            entry.value.condition.apply(entry.key, currentCh, entry.value.ignoreCase)
        }
    }
}

private fun checkArgs(s: CharSequence, start: Int, end: Int) {
    require(start >= 0) {"start ($start) < 0"}
    require(start <= end) {"start ($start) > end ($end)"}
    require(end <= s.length) {"end ($end) > length (${s.length})"}
}

data class CharConditionInfo(
        val condition: CompareCondition,
        val ignoreCase: Boolean = true
)
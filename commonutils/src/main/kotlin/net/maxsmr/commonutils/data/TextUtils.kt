package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.number.toDoubleNoThrow
import java.util.*

fun isEmpty(s: CharSequence?): Boolean {
    return isEmpty(s, false)
}

fun isEmpty(s: CharSequence?, shouldCheckNullString: Boolean): Boolean {
    return s == null || s == EMPTY_STRING || shouldCheckNullString && "null".equals(s.toString(), ignoreCase = true)
}

fun isZeroOrNull(value: String?) = net.maxsmr.commonutils.data.number.isZeroOrNull(toDoubleNoThrow(value))

fun isNotZeroOrNull(value: String?) = !isZeroOrNull(value)

// Copied from TextUtils
fun join(delimiter: CharSequence, tokens: Array<Any?>): String {
    return join(delimiter, tokens.toList())
}

// Copied from TextUtils
fun join(delimiter: CharSequence, tokens: Iterable<*>): String {
    val it = tokens.iterator()
    if (!it.hasNext()) {
        return ""
    }
    val sb = StringBuilder()
    sb.append(it.next())
    while (it.hasNext()) {
        sb.append(delimiter)
        sb.append(it.next())
    }
    return sb.toString()
}

// Copied from TextUtils
fun split(text: String, expression: String): Array<String?>? {
    return if (text.isEmpty()) {
        arrayOf()
    } else {
        text.split(expression).dropLastWhile { it.isEmpty() }.toTypedArray()
    }
}

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

fun replaceRange(s: CharSequence, start: Int, end: Int, replacement: CharSequence): String {
    if (start < 0) throw StringIndexOutOfBoundsException("start < 0")
    if (start > end) throw StringIndexOutOfBoundsException("start > end")
    if (end > s.length) throw StringIndexOutOfBoundsException("end > length")
    val sb = StringBuilder(s)
    sb.replace(start, end, replacement.toString())
    return sb.toString()
}

fun removeSubstings(
        text: String?,
        characters: Set<String?>?
) = replaceSubstrings(text, characters)

fun removeRange(s: CharSequence, start: Int, end: Int): String {
    if (start < 0) throw StringIndexOutOfBoundsException("start < 0")
    if (start > end) throw StringIndexOutOfBoundsException("start > end")
    if (end > s.length) throw StringIndexOutOfBoundsException("end > length")
    val sb = StringBuilder(s)
    sb.delete(start, end)
    return sb.toString()
}

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

/**
 * Убирает указанные символы с начала или конца строки
 * @param shouldNotLeaveEmpty не оставлять пустым, если все символы были исключены
 */
fun removeChars(
        text: String,
        isFromStart: Boolean,
        shouldNotLeaveEmpty: Boolean,
        charsToExclude: List<Char?>?
): String? {
    val result = StringBuilder()
    var shouldAppendText = false
    if (charsToExclude != null && charsToExclude.isNotEmpty()) {
        var wasAppended = false
        if (isFromStart) {
            for (element in text) {
                val current = element
                if (wasAppended || !charsToExclude.contains(current)) {
                    result.append(current)
                    wasAppended = true
                }
            }
        } else {
            for (i in text.length - 1 downTo 0) {
                val current = text[i]
                if (wasAppended || !charsToExclude.contains(current)) {
                    result.append(current)
                    wasAppended = true
                }
            }
            result.reverse()
        }
    } else {
        shouldAppendText = true
    }
    if (shouldNotLeaveEmpty && result.isEmpty()) {
        shouldAppendText = true
    }
    if (shouldAppendText) {
        result.append(text)
    }
    return result.toString()
}

fun trim(cs: CharSequence?): String {
    return trim(cs, true, true)
}

fun trim(cs: CharSequence?, fromStart: Boolean, fromEnd: Boolean): String {
    return trim(cs, CompareCondition.LESS_OR_EQUAL, ' ', fromStart, fromEnd)
}

fun trim(cs: CharSequence?, compareCondition: CompareCondition, byChar: Char, fromStart: Boolean, fromEnd: Boolean): String {
    if (cs == null) {
        return EMPTY_STRING
    }
    val str = cs.toString()
    var len = str.length
    var st = 0
    if (fromStart) {
        while (st < len && compareCondition.apply(str[st], byChar, false)) {
            st++
        }
    }
    if (fromEnd) {
        while (st < len && compareCondition.apply(str[len - 1], byChar, false)) {
            len--
        }
    }
    return if (st > 0 || len < str.length) str.substring(st, len) else str
}

fun getStubValue(value: String?, stringsProvider: () -> String): String? {
    return getStubValueWithAppend(value, null, stringsProvider)
}

fun getStubValue(value: Int, stringsProvider: () -> String): String? {
    return getStubValueWithAppend(value, null, stringsProvider)
}

fun getStubValueWithAppend(value: Int, appendWhat: String?, stringsProvider: () -> String): String? {
    return getStubValueWithAppend(if (value != 0) value.toString() else null, appendWhat, stringsProvider)
}

fun getStubValueWithAppend(value: String?, appendWhat: String?, stringsProvider: () -> String): String? {
    return if (!isEmpty(value)) if (!isEmpty(appendWhat)) "$value $appendWhat" else value else stringsProvider.invoke()
}
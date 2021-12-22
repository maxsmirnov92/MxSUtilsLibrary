package net.maxsmr.commonutils.text

import net.maxsmr.commonutils.CompareCondition
import net.maxsmr.commonutils.Predicate
import net.maxsmr.commonutils.conversion.toDoubleOrNull
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.number.isZeroOrNull
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("TextUtils")

fun getExtension(name: String?): String {
    if (name == null) return EMPTY_STRING
    val index = name.lastIndexOf('.')
    return if (index > 0 && index < name.length - 1) name.substring(index + 1) else EMPTY_STRING
}

/**
 * убрать расширение файла
 *
 * @return новое имя
 */
fun removeExtension(name: String?): String {
    var result: String = name ?: EMPTY_STRING
    if (name != null && name.isNotEmpty()) {
        val startIndex = name.lastIndexOf('.')
        if (startIndex >= 0) {
            result = replaceRange(name, startIndex, name.length, EMPTY_STRING).toString()
        }
    }
    return result
}

/**
 * Дописать расширение; убирает существующее, если есть
 */
fun appendOrReplaceExtension(name: String?, extension: String?): String {
    name?.let {
        val newName = removeExtension(name)
        if (newName.isNotEmpty()) {
            return if (!isEmpty(extension)) {
                "$newName.$extension"
            } else {
                newName
            }
        }
    }
    return EMPTY_STRING
}

/**
 * Дописать в конец имени (до расширения ".", если есть) [postfix]
 */
fun appendPostfix(name: String?, postfix: String?): String {
    if (!isEmpty(postfix) && name != null) {
        var newName = removeExtension(name) + postfix
        val extension = getExtension(name)
        if (extension.isNotEmpty()) {
            newName += ".$extension"
        }
        return newName
    }
    return name ?: EMPTY_STRING
}

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
    s == null || s.isEmpty() || shouldCheckNullString && "null".equals(
        s.toString(),
        ignoreCase = true
    )

@JvmOverloads
fun orEmpty(s: CharSequence?, shouldCheckNullString: Boolean = false): CharSequence =
    if (s == null || isEmpty(s, shouldCheckNullString)) EMPTY_STRING else s

@JvmOverloads
fun orEmpty(s: String?, shouldCheckNullString: Boolean = false): String =
    if (s == null || isEmpty(s, shouldCheckNullString)) EMPTY_STRING else s

fun isZeroOrNull(value: CharSequence?) = value.toDoubleOrNull().isZeroOrNull()

fun isNotZeroOrNull(value: CharSequence?) = !isZeroOrNull(value)

fun getStubValue(value: String?, stringsProvider: () -> String): String? =
    getStubValueWithAppend(value, null, stringsProvider)

fun getStubValue(value: Int, stringsProvider: () -> String): String? =
    getStubValueWithAppend(value, null, stringsProvider)

fun getStubValueWithAppend(
    value: Int,
    appendWhat: String?,
    stringsProvider: () -> String
): String? =
    getStubValueWithAppend(if (value != 0) value.toString() else null, appendWhat, stringsProvider)

fun getStubValueWithAppend(
    value: String?,
    appendWhat: String?,
    stringsProvider: () -> String
): String? =
    if (!isEmpty(value)) if (!isEmpty(appendWhat)) "$value $appendWhat" else value else stringsProvider.invoke()

@JvmOverloads
fun CharSequence?.capFirstChar(
    tailToLowerCase: Boolean = true,
    locale: Locale = Locale.getDefault()
) {
    changeCaseFirstChar(
        isUpperForFirst = true,
        charCaseForTail = if (tailToLowerCase) CharCase.LOWER else CharCase.NO_CHANGE,
        locale = locale
    )
}

/**
 * Приводит первый символ к верхнему или нижнему регистру, а оставшиеся - меняет
 *
 * @return строка с большой буквы
 */
fun CharSequence?.changeCaseFirstChar(
    isUpperForFirst: Boolean,
    charCaseForTail: CharCase,
    locale: Locale = Locale.getDefault()
): String {

    fun String.changeCase(isUpper: Boolean) =
        if (isUpper) toUpperCase(locale) else toLowerCase(locale)

    var result = EMPTY_STRING
    if (this != null && this.isNotEmpty()) {
        result = this.toString()
        result = if (this.length == 1) {
            result.changeCase(isUpperForFirst)
        } else {
            val tail = result.substring(1)
            (result.substring(0, 1).changeCase(isUpperForFirst)
                    + (if (charCaseForTail == CharCase.NO_CHANGE) {
                tail
            } else {
                tail.changeCase(charCaseForTail == CharCase.UPPER)
            }))
        }
    }
    return result
}

enum class CharCase {
    UPPER, LOWER, NO_CHANGE
}

fun insertAt(target: CharSequence, index: Int, what: CharSequence): String {
    require(!(index < 0 || index >= target.length)) { "incorrect index: $index" }
    return target.toString().substring(0, index) + what + target.toString()
        .substring(index, target.length)
}

fun indexOf(s: CharSequence, c: Char): Int {
    for (index in s.indices) {
        if (s[index] == c) {
            return index
        }
    }
    return -1
}

/**
 * Аналогично [CharSequence.indexOf], только возвращает список индексов **всех** вхождений подстроки в строку
 */
@JvmOverloads
fun CharSequence.indicesOf(
    substring: String,
    startIndex: Int = 0,
    ignoreCase: Boolean = false
): List<Int> {
    val positions = mutableListOf<Int>()
    var searchIndex = startIndex
    var resultIndex: Int

    do {
        resultIndex = this.indexOf(substring, searchIndex, ignoreCase)
        if (resultIndex != -1) {
            positions.add(resultIndex)
        }
        searchIndex = resultIndex + substring.length
    } while (resultIndex != -1)
    return positions
}

@JvmOverloads
fun CharSequence.substringBefore(
    delimiter: Char,
    missingDelimiterValue: CharSequence = this
): CharSequence {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
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

fun replaceRange(s: CharSequence, start: Int, end: Int, replacement: CharSequence): CharSequence =
    try {
        replaceRangeOrThrow(s, start, end, replacement)
    } catch (e: IllegalArgumentException) {
        s
    }

fun replaceRangeOrThrow(s: CharSequence, start: Int, end: Int, replacement: CharSequence): String {
    checkArgs(s, start, end)
    val sb = StringBuilder(s)
    sb.replace(start, end, replacement.toString())
    return sb.toString()
}

fun removeRange(s: CharSequence, start: Int, end: Int): CharSequence =
    try {
        removeRangeOrThrow(s, start, end)
    } catch (e: IllegalArgumentException) {
        s
    }

fun removeRangeOrThrow(s: CharSequence, start: Int, end: Int): String {
    checkArgs(s, start, end)
    val sb = StringBuilder(s)
    sb.delete(start, end)
    return sb.toString()
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
): CharSequence = removeChars(
    text,
    isFromStart,
    mapOf(Pair(byChar, CharConditionInfo(condition, ignoreCase))),
    andRule,
    excludeByConditions
)

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
    trimDirection: TrimDirection,
    byChar: Char,
    condition: CompareCondition = CompareCondition.LESS_OR_EQUAL,
    ignoreCase: Boolean = true,
    andRule: Boolean = false,
    excludeByConditions: Boolean = true
): CharSequence =
    trimWithCondition(
        text,
        trimDirection,
        mapOf(Pair(byChar, CharConditionInfo(condition, ignoreCase))),
        andRule,
        excludeByConditions
    )

fun trimWithCondition(
    text: CharSequence,
    trimDirection: TrimDirection,
    conditions: Map<Char, CharConditionInfo>,
    andRule: Boolean = false,
    excludeByConditions: Boolean = true
): CharSequence =
    trim(text, trimDirection, excludeByConditions) { ch, index ->
        isTrue(conditions, andRule, ch)
    }

@JvmOverloads
fun trimByLength(
    text: CharSequence,
    targetLength: Int,
    postfix: CharSequence = EMPTY_STRING
): CharSequence {
    if (targetLength <= 0 || text.length < targetLength) {
        return text
    }
//    val result = trim(text,  TrimDirection.END, false) { _, index ->
//        index >= targetLength
//    }
    return text.substring(0, targetLength) + postfix
}

/**
 * Returns a sub sequence of this char sequence having leading/trailing characters matching the [predicate] removed.
 */
fun trim(
    text: CharSequence,
    trimDirection: TrimDirection,
    excludeByPredicate: Boolean = true,
    predicate: (Char, Int) -> Boolean
): CharSequence {

    fun CharSequence.trim(fromStart: Boolean): CharSequence {
        for (index in if (fromStart) this.indices else this.indices.reversed()) {
            val ch = this[index]
            val isTrue = predicate(ch, index)
            if (if (excludeByPredicate) !isTrue else isTrue) {
                return if (fromStart) {
                    this.subSequence(index, this.length)
                } else {
                    this.subSequence(0, index + 1)
                }
            }
        }
        return this
    }

    return when (trimDirection) {
        TrimDirection.START -> {
            text.trim(true)
        }
        TrimDirection.END -> {
            text.trim(false)
        }
        TrimDirection.BOTH -> {
            text.trim(true).trim(false)
        }
    }
}

fun appendSubstringWhileLess(
    text: CharSequence,
    minCharsCount: Int,
    fromStart: Boolean,
    substring: String
): CharSequence {
    val result = StringBuilder(text)
    if (substring.isNotEmpty()) {
        while (result.length < minCharsCount) {
            if (fromStart) {
                result.insert(0, substring)
            } else {
                result.append(substring)
            }
        }
    }
    return result
}

/**
 * Убрать из строки такое кол-во символов, не соответствующих [limitedChars], чтобы общий размер не превышал [targetTextSize]
 * @param checkClearedTextSize необходимость проверки строки после преобразований на соответствие [targetTextSize] (т.к. могло быть превышено за счёт незапретных символов)
 */
@JvmOverloads
fun removeChars(
    text: CharSequence,
    targetTextSize: Int,
    limitedChars: Collection<Char>,
    checkClearedTextSize: Boolean = false
): String {
    require(targetTextSize > 0) { "Incorrect target size: $targetTextSize" }
    val result = StringBuilder()
    val array = text.toString().toCharArray()
    // кол-во символов для исключения из целевой строки, чтобы получить targetSize
    val removedCharsCount = if (text.length > targetTextSize) text.length - targetTextSize else 0
    // кол-во символов для ограничения в целевой строке
    val limitedCharsCount = array.toList().filter { limitedChars.contains(it) }.size
    // максимально возможное кол-во ограничиваемых символов
    val targetLimitedCharsCount =
        if (limitedCharsCount > removedCharsCount) limitedCharsCount - removedCharsCount else 0
    var currentLimitedCharsCount = 0
    array.forEach {
        // является ограничиваемым
        val isLimited = limitedChars.contains(it)
        if (!isLimited || currentLimitedCharsCount < targetLimitedCharsCount) {
            result.append(it)
            if (isLimited) {
                currentLimitedCharsCount++
            }
        }
    }
    if (checkClearedTextSize && result.length > targetTextSize) {
        result.replace(targetTextSize - 1, result.length, EMPTY_STRING)
    }
    return result.toString()
}

@JvmOverloads
fun createPlaceholderText(size: Int, placeholderChar: Char = ' '): String {
    require(size > 0) { "Incorrect size: $size" }
    val result = StringBuilder()
    for (i in 0 until size) {
        result.append(placeholderChar)
    }
    return result.toString()
}

/**
 * @param allowedChars разрешённые символы, из которых сделать регулярки для валидации и фильтрации
 * @param allowIfEmpty true, если при пустой разрешённой строке вернуть исходную [this]; в ином случае - пустую
 * @return модифицированная или исходная строка
 */
@JvmOverloads
fun CharSequence?.filterTextByRegex(
    allowedChars: String,
    allowIfEmpty: Boolean = false
): CharSequence {
    if (this == null || this.isEmpty()) {
        return EMPTY_STRING
    }
    if (allowedChars.isEmpty()) {
        return if (allowIfEmpty) {
            this
        } else {
            EMPTY_STRING
        }
    }
    val matchRegex = "[-$allowedChars]+".toRegex()
    val editRegex = "[^$allowedChars]+".toRegex()
    val text = this.toString()
    return if (text.matches(matchRegex)) {
        this
    } else {
        text.replace(editRegex, EMPTY_STRING)
    }
}

@JvmOverloads
fun getBytes(text: String, charset: Charset = Charsets.UTF_8): ByteArray? = try {
    text.toByteArray(charset)
} catch (e: UnsupportedEncodingException) {
    logger.e(formatException(e, "toByteArray"))
    null
}

fun charsetForNameOrNull(name: String?) =
    try {
        Charset.forName(name)
    } catch (e: IllegalArgumentException) {
        logger.e(formatException(e, "Charset.forName"))
        null
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
    require(start >= 0) { "start ($start) < 0" }
    require(start <= end) { "start ($start) > end ($end)" }
    require(end <= s.length) { "end ($end) > length (${s.length})" }
}

data class CharConditionInfo(
    val condition: CompareCondition,
    val ignoreCase: Boolean = true
)

enum class TrimDirection {
    START, END, BOTH
}
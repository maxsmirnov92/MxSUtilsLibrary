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
import java.util.Locale

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("TextUtils")

fun String?.getExtension() = this?.substringAfterLast('.', missingDelimiterValue = EMPTY_STRING).orEmpty()

/**
 * убрать расширение файла
 *
 * @return новое имя
 */
fun String?.removeExtension(): String {
     return this?.substringBeforeLast('.').orEmpty()
//    if (!this.isNullOrEmpty()) {
//        val startIndex = this.lastIndexOf('.')
//        if (startIndex >= 0) {
//            return replaceRange(startIndex, this.length, EMPTY_STRING).toString()
//        }
//    }
//    return EMPTY_STRING
}

/**
 * Дописать расширение
 * @param shouldReplace true для убирания существующего
 */
@JvmOverloads
fun String?.appendExtension(
    extension: String?,
    shouldReplace: Boolean = true
): String {
    this?.let {
        val newName = if (shouldReplace) removeExtension() else this
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
fun String?.appendPostfix(postfix: String?): String {
    if (!isEmpty(postfix) && this != null) {
        var newName = removeExtension() + postfix
        val extension = getExtension()
        if (extension.isNotEmpty()) {
            newName += ".$extension"
        }
        return newName
    }
    return this.orEmpty()
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
    s.isNullOrEmpty() || shouldCheckNullString && "null".equals(
        s.toString(),
        ignoreCase = true
    )

@JvmOverloads
fun orEmpty(s: CharSequence?, shouldCheckNullString: Boolean = false): CharSequence =
    if (s == null || isEmpty(s, shouldCheckNullString)) EMPTY_STRING else s

@JvmOverloads
fun orEmpty(s: String?, shouldCheckNullString: Boolean = false): String =
    if (s == null || isEmpty(s, shouldCheckNullString)) EMPTY_STRING else s

fun CharSequence?.isZeroOrNull() = toDoubleOrNull().isZeroOrNull()

fun CharSequence?.isNotZeroOrNull() = !isZeroOrNull()

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
    charCaseForTail: CharCase = CharCase.NO_CHANGE,
    locale: Locale = Locale.getDefault()
): String = changeCaseFirstChar(
    isUpperForFirst = true,
    charCaseForTail = charCaseForTail,
    locale = locale
)

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
        if (isUpper) uppercase(locale) else lowercase(locale)

    var result = EMPTY_STRING
    if (!this.isNullOrEmpty()) {
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

fun String.insertAt(index: Int, what: CharSequence): String {
    require(!(index < 0 || index >= this.length)) { "incorrect index: $index" }
    return this.substring(0, index) + what + this
        .substring(index, this.length)
}

fun CharSequence?.indexOf(c: Char): Int {
    this?.let {
        for (index in indices) {
            if (this[index] == c) {
                return index
            }
        }
    }
    return -1
}

/**
 * Аналогично [CharSequence.indexOf], только возвращает список индексов **всех** вхождений подстроки в строку
 */
@JvmOverloads
fun CharSequence?.indicesOf(
    substring: String,
    startIndex: Int = 0,
    ignoreCase: Boolean = false
): List<Int> {
    val positions = mutableListOf<Int>()
    if (this != null) {
        var searchIndex = startIndex
        var resultIndex: Int
        do {
            resultIndex = this.indexOf(substring, searchIndex, ignoreCase)
            if (resultIndex != -1) {
                positions.add(resultIndex)
            }
            searchIndex = resultIndex + substring.length
        } while (resultIndex != -1)
    }
    return positions
}

@JvmOverloads
fun CharSequence?.substringBefore(
    delimiter: Char,
    missingDelimiterValue: CharSequence? = this
): CharSequence? {
    if (this == null) return missingDelimiterValue
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

fun String?.replaceSubstrings(
    substrings: Set<String>?,
    replacement: String = EMPTY_STRING
): String? {
    var result = this
    if (substrings != null)
        for (c in substrings) {
            if (result != null && !isEmpty(result)) {
                result = result.replace(c, replacement)
            }
        }
    return result
}

fun CharSequence.replaceRange(start: Int, end: Int, replacement: CharSequence): CharSequence =
    try {
        replaceRangeOrThrow(start, end, replacement)
    } catch (e: IllegalArgumentException) {
        this
    }

fun CharSequence.replaceRangeOrThrow(start: Int, end: Int, replacement: CharSequence): String {
    checkArgs(start, end)
    val sb = StringBuilder(this)
    sb.replace(start, end, replacement.toString())
    return sb.toString()
}

fun CharSequence.removeRange(start: Int, end: Int): CharSequence =
    try {
        removeRangeOrThrow(start, end)
    } catch (e: IllegalArgumentException) {
        this
    }

fun CharSequence.removeRangeOrThrow(start: Int, end: Int): String {
    checkArgs(start, end)
    val sb = StringBuilder(this)
    sb.delete(start, end)
    return sb.toString()
}

fun String?.removeSubstrings(substrings: Set<String>?) = replaceSubstrings(substrings, EMPTY_STRING)

fun CharSequence.removeNonDigits(): CharSequence {
    val format = StringBuilder(this)
    var i = 0
    while (i < format.length) {
        if (!Character.isDigit(format[i])) format.deleteCharAt(i) else i++
    }
    return format
}

fun CharSequence.removeCharAt(index: Int): String {
    if (index < 0 || index >= this.length) throw StringIndexOutOfBoundsException("Incorrect index: $index")
    val sb = StringBuilder(this)
    sb.deleteCharAt(index)
    return sb.toString()
}

fun CharSequence.removeChars(
    isFromStart: Boolean,
    byChar: Char,
    condition: CompareCondition = CompareCondition.LESS_OR_EQUAL,
    ignoreCase: Boolean = true,
    andRule: Boolean = false,
    excludeByConditions: Boolean = true
): CharSequence = removeChars(
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
fun CharSequence.removeChars(
    isFromStart: Boolean,
    conditions: Map<Char, CharConditionInfo>,
    andRule: Boolean = false,
    excludeByConditions: Boolean = true
): CharSequence =
    removeChars(isFromStart, excludeByConditions) {
        isTrue(conditions, andRule, it)
    }

fun CharSequence.removeChars(
    isFromStart: Boolean,
    excludeByConditions: Boolean = true,
    predicate: (Char) -> Boolean
): CharSequence {
    val result = StringBuilder()
    var wasAppended = false
    if (isFromStart) {
        for (current in this) {
            val isTrue = predicate(current)
            if (wasAppended || if (excludeByConditions) !isTrue else isTrue) {
                result.append(current)
                wasAppended = true
            }
        }
    } else {
        for (i in this.length - 1 downTo 0) {
            val current = this[i]
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

fun CharSequence.trimWithCondition(
    trimDirection: TrimDirection,
    byChar: Char,
    condition: CompareCondition = CompareCondition.LESS_OR_EQUAL,
    ignoreCase: Boolean = true,
    andRule: Boolean = false,
): CharSequence =
    trimWithCondition(
        trimDirection,
        mapOf(Pair(byChar, CharConditionInfo(condition, ignoreCase))),
        andRule
    )

fun CharSequence.trimWithCondition(
    trimDirection: TrimDirection,
    conditions: Map<Char, CharConditionInfo>,
    andRule: Boolean = false,
): CharSequence =
    trim(trimDirection) { ch, index ->
        isTrue(conditions, andRule, ch)
    }

@JvmOverloads
fun CharSequence.trimByLength(
    targetLength: Int,
    postfix: CharSequence = EMPTY_STRING
): CharSequence {
    if (targetLength <= 0 || length < targetLength) {
        return this
    }
//    val result = trim(text,  TrimDirection.END, false) { _, index ->
//        index >= targetLength
//    }
    return substring(0, targetLength) + postfix
}

/**
 * Returns a sub sequence of this char sequence having leading/trailing characters matching the [predicate] removed.
 */
fun CharSequence.trim(
    trimDirection: TrimDirection,
    predicate: (Char, Int) -> Boolean
): CharSequence {

    fun CharSequence.trim(fromStart: Boolean): CharSequence {
        for (index in if (fromStart) this.indices else this.indices.reversed()) {
            val ch = this[index]
            if (predicate(ch, index)) {
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
            trim(true)
        }

        TrimDirection.END -> {
            trim(false)
        }

        TrimDirection.BOTH -> {
            trim(true).trim(false)
        }
    }
}

fun CharSequence.appendSubstringWhileLess(
    minCharsCount: Int,
    fromStart: Boolean,
    substring: String
): CharSequence {
    val result = StringBuilder(this)
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
fun String.removeChars(
    targetTextSize: Int,
    limitedChars: Collection<Char>,
    checkClearedTextSize: Boolean = false
): String {
    require(targetTextSize > 0) { "Incorrect target size: $targetTextSize" }
    val result = StringBuilder()
    val array = this.toCharArray()
    // кол-во символов для исключения из целевой строки, чтобы получить targetSize
    val removedCharsCount = if (this.length > targetTextSize) this.length - targetTextSize else 0
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
    if (this.isNullOrEmpty()) {
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
fun String.getBytes(charset: Charset = Charsets.UTF_8): ByteArray? = try {
    toByteArray(charset)
} catch (e: UnsupportedEncodingException) {
    logger.e(formatException(e, "toByteArray"))
    null
}

fun String?.charsetForNameOrNull() =
    try {
        Charset.forName(this)
    } catch (e: IllegalArgumentException) {
        logger.e(formatException(e, "Charset.forName: $this"))
        null
    }

private fun isTrue(
    conditions: Map<Char, CharConditionInfo>,
    andRule: Boolean,
    currentChar: Char
): Boolean {
    return if (andRule) {
        if (conditions.isEmpty()) {
            return false
        } else {
            !Predicate.Methods.contains(conditions.entries) { entry ->
                !entry.value.condition.apply(entry.key, currentChar, entry.value.ignoreCase)
            }
        }
    } else {
        Predicate.Methods.contains(conditions.entries) { entry ->
            entry.value.condition.apply(entry.key, currentChar, entry.value.ignoreCase)
        }
    }
}

private fun CharSequence.checkArgs(start: Int, end: Int) {
    require(start >= 0) { "start ($start) < 0" }
    require(start <= end) { "start ($start) > end ($end)" }
    require(end <= length) { "end ($end) > length (${length})" }
}

data class CharConditionInfo(
    val condition: CompareCondition,
    val ignoreCase: Boolean = true
)

enum class TrimDirection {
    START, END, BOTH
}
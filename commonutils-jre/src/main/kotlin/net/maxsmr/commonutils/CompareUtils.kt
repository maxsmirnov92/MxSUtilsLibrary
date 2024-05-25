package net.maxsmr.commonutils

import net.maxsmr.commonutils.MatchStringOption.*
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty
import net.maxsmr.commonutils.text.join
import java.util.*

// may be usesul in Java, where Objects.equal() not allowed
fun objectsEqual(one: Any?, another: Any?): Boolean =
    if (one != null) one == another else another == null

@JvmOverloads
fun charsEqual(one: Char?, another: Char?, ignoreCase: Boolean = false): Boolean {
    var one = one
    var another = another
    if (ignoreCase) {
        one = one?.toLowerCase()
        another = another?.toLowerCase()
    }
    return if (one != null) one == another else another == null
}

@JvmOverloads
fun stringsEqual(one: String?, another: String?, ignoreCase: Boolean = false): Boolean =
    if (!ignoreCase) objectsEqual(one, another) else one.equals(another, ignoreCase = true)

@JvmOverloads
fun compareForNull(lhs: Any?, rhs: Any?, ascending: Boolean = true): Int =
    if (ascending) if (lhs != null) if (rhs != null) 0 else 1 else if (rhs == null) 0 else -1 else if (lhs != null) if (rhs != null) 0 else -1 else if (rhs == null) 0 else 1

@JvmOverloads
fun <C : Comparable<C>?> compareObjects(one: C?, another: C?, ascending: Boolean = true): Int =
    if (one != null) if (another != null) if (ascending) one.compareTo(another) else another.compareTo(one) else 1 else if (another == null) 0 else -1

@JvmOverloads
fun compareNumbers(one: Number?, another: Number?, ascending: Boolean = true): Int =
    compareDoubles(one?.toDouble(), another?.toDouble(), ascending)

@JvmOverloads
fun compareInts(one: Int?, another: Int?, ascending: Boolean = true): Int =
    compareLongs(one?.toLong(), another?.toLong(), ascending)

@JvmOverloads
fun compareLongs(one: Long?, another: Long?, ascending: Boolean = true): Int =
    compareObjects(one, another, ascending)

@JvmOverloads
fun compareFloats(one: Float?, another: Float?, ascending: Boolean = true): Int =
    compareDoubles(one?.toDouble(), another?.toDouble(), ascending)

@JvmOverloads
fun compareDoubles(one: Double?, another: Double?, ascending: Boolean = true): Int =
    compareObjects(one, another, ascending)

@JvmOverloads
fun compareChars(one: Char?, another: Char?, ascending: Boolean = true, ignoreCase: Boolean = false): Int {
    val one = if (ignoreCase) one?.toLowerCase() else one
    val another = if (ignoreCase) another?.toLowerCase() else another
    return compareDoubles(one?.toDouble(), another?.toDouble())
}

@JvmOverloads
fun compareStrings(one: String?, another: String?, ascending: Boolean = true, ignoreCase: Boolean = false): Int {
    val one = if (ignoreCase) one?.toLowerCase(Locale.getDefault()) else one
    val another = if (ignoreCase) another?.toLowerCase(Locale.getDefault()) else another
    return compareObjects(one, another, ascending)
}

@JvmOverloads
fun compareDates(one: Date?, another: Date?, ascending: Boolean = true): Int =
    compareLongs(one?.time ?: 0, another?.time ?: 0, ascending)


/**
 * @param initialText строка, в которой ищутся вхождения
 * @param matchText   сопостовляемая строка
 * @param matchFlags  флаги, собранные из [MatchStringOption] для выбора опций поиска соответствий
 * @param separators  опциональные разделители для разбивки на подстроки в режиме [MatchStringOption.AUTO] или [MatchStringOption.AUTO_IGNORE_CASE]
 * @return true если соответствие найдено, false - в противном случае
 */
@JvmOverloads
fun stringsMatch(
    initialText: String?,
    matchText: String?,
    matchFlags: Int = AUTO_IGNORE_CASE.flag,
    vararg separators: String?
): Boolean {

    val one = initialText.orEmpty()
    val another = matchText.orEmpty()
    val oneIgnoreCase = one.lowercase(Locale.getDefault())
    val anotherIgnoreCase = another.lowercase(Locale.getDefault())

    var match = false
    if (MatchStringOption.contains(EQUALS, matchFlags)) {
        if (one == another) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(EQUALS_IGNORE_CASE, matchFlags)) {
        if (one.equals(another, ignoreCase = true)) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(CONTAINS, matchFlags)) {
        if (one.contains(another)) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(CONTAINS_IGNORE_CASE, matchFlags)) {
        if (oneIgnoreCase.contains(anotherIgnoreCase)) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(STARTS_WITH, matchFlags)) {
        if (one.startsWith(another)) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(STARTS_WITH_IGNORE_CASE, matchFlags)) {
        if (oneIgnoreCase.startsWith(anotherIgnoreCase)) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(END_WITH, matchFlags)) {
        if (one.endsWith(another)) {
            match = true
        }
    }
    if (!match && MatchStringOption.contains(END_WITH_IGNORE_CASE, matchFlags)) {
        if (oneIgnoreCase.endsWith(anotherIgnoreCase)) {
            match = true
        }
    }
    if (!match && (MatchStringOption.contains(AUTO, matchFlags) || MatchStringOption.contains(AUTO_IGNORE_CASE, matchFlags))) {
        if (!isEmpty(one)) {
            val isIgnoreCase = MatchStringOption.contains(AUTO_IGNORE_CASE, matchFlags)
            val trimmedOne = (if (isIgnoreCase) oneIgnoreCase else one).trim { it <= ' ' }
            val trimmedAnother = (if (isIgnoreCase) anotherIgnoreCase else another).trim { it <= ' ' }
            if (stringsEqual(trimmedOne, trimmedAnother, false)) {
                match = true
            } else {
                val parts = trimmedOne.split("[" + (if (separators.isNotEmpty()) join(EMPTY_STRING, separators.toList()) else " ") + "]+")
                if (parts.isNotEmpty()) {
                    for (word in parts) {
                        val word = if (isIgnoreCase) {
                            word.lowercase(Locale.getDefault())
                        } else {
                            word
                        }
                        if (!MatchStringOption.containsAny(matchFlags, MatchStringOption.valuesExceptOf(listOf(AUTO, AUTO_IGNORE_CASE)))) {
                            if (word.startsWith(trimmedAnother) || word.endsWith(trimmedAnother)) {
                                match = true
                                break
                            }
                        } else if (parts.size > 1) {
                            if (stringsMatch(word, trimmedAnother, MatchStringOption.resetFlags(matchFlags, listOf(AUTO, AUTO_IGNORE_CASE)))) {
                                match = true
                                break
                            }
                        }
                    }
                }
            }
        }
    }
    return match
}

enum class MatchStringOption(val flag: Int) {
    AUTO(1),
    AUTO_IGNORE_CASE(1 shl 1),
    EQUALS(1 shl 2),
    EQUALS_IGNORE_CASE(1 shl 3),
    CONTAINS(1 shl 4),
    CONTAINS_IGNORE_CASE(1 shl 5),
    STARTS_WITH(1 shl 6),
    STARTS_WITH_IGNORE_CASE(1 shl 7),
    END_WITH(1 shl 8),
    END_WITH_IGNORE_CASE(1 shl 9);

    val isIgnoreCase: Boolean get() = this == AUTO_IGNORE_CASE
            || this == EQUALS_IGNORE_CASE
            || this == CONTAINS_IGNORE_CASE
            || this == STARTS_WITH_IGNORE_CASE
            || this == END_WITH_IGNORE_CASE

    companion object {

        fun contains(option: MatchStringOption, flags: Int): Boolean {
            return flags and option.flag == option.flag
        }

        fun containsAny(flags: Int, options: Collection<MatchStringOption>?): Boolean {
            var contains = false
            if (options != null) {
                for (o in options) {
                    if (contains(o, flags)) {
                        contains = true
                        break
                    }
                }
            }
            return contains
        }

        fun setFlag(flags: Int, option: MatchStringOption): Int {
            return flags or option.flag
        }

        fun setFlags(flags: Int, options: Collection<MatchStringOption?>?): Int {
            var flags = flags
            if (options != null) {
                for (o in options) {
                    if (o != null) {
                        flags = setFlag(flags, o)
                    }
                }
            }
            return flags
        }

        fun resetFlag(flags: Int, option: MatchStringOption): Int {
            return flags and option.flag.inv()
        }

        fun resetFlags(flags: Int, options: Collection<MatchStringOption?>?): Int {
            var flags = flags
            if (options != null) {
                for (o in options) {
                    if (o != null) {
                        flags = resetFlag(flags, o)
                    }
                }
            }
            return flags
        }

        fun valuesExceptOf(exclude: Collection<MatchStringOption?>?): Set<MatchStringOption> {
            val result: MutableSet<MatchStringOption> = LinkedHashSet()
            result.addAll(Arrays.asList(*values()))
            if (exclude != null) {
                result.removeAll(exclude)
            }
            return result
        }
    }

}

enum class CompareCondition {

    LESS, LESS_OR_EQUAL, EQUAL, MORE, MORE_OR_EQUAL;

    fun apply(one: Char?, another: Char?, ignoreCase: Boolean): Boolean {
        return fromResult(compareChars(one, another, true, ignoreCase), this)
    }

    fun apply(one: String?, another: String?, ignoreCase: Boolean): Boolean {
        return fromResult(compareStrings(one, another, true, ignoreCase), this)
    }

    fun apply(one: Number?, another: Number?): Boolean {
        return fromResult(compareDoubles(one?.toDouble(), another?.toDouble(), true), this)
    }

    companion object {

        fun fromResult(result: Int, c: CompareCondition): Boolean {
            return when (c) {
                LESS -> result < 0
                LESS_OR_EQUAL -> result <= 0
                EQUAL -> result == 0
                MORE -> result > 0
                MORE_OR_EQUAL -> result >= 0
            }
        }
    }
}
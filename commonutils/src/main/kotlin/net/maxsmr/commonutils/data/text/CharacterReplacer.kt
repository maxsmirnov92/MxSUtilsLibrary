package net.maxsmr.commonutils.data.text

import net.maxsmr.commonutils.data.charsEqual
import java.util.*

object CharacterReplacer {

    private val replaceLatinCyrillicAlphabet = mutableListOf<CharacterMap>()

    init {
        replaceLatinCyrillicAlphabet.add(CharacterMap('a', 'а'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('b', 'б'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('c', 'ц'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('d', 'д'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('e', 'е'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('f', 'ф'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('g', 'г'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('h', 'х'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('i', 'и'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('j', 'ж'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('k', 'к'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('l', 'л'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('m', 'м'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('n', 'н'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('o', 'о'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('p', 'п'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('q', 'к'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('r', 'р'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('s', 'с'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('t', 'т'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('u', 'у'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('v', 'в'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('w', 'в'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('x', 'х'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('y', 'у'))
        replaceLatinCyrillicAlphabet.add(CharacterMap('z', 'з'))
    }

    fun findCharByLeft(alphabet: List<CharacterMap>, c: Char, ignoreCase: Boolean): Char? {
        return alphabet.find { charsEqual(c, it.rightCh, ignoreCase) }?.leftCh
    }

    fun findCharByRight(alphabet: List<CharacterMap>, c: Char, ignoreCase: Boolean): Char? {
        return alphabet.find { charsEqual(c, it.rightCh, ignoreCase) }?.leftCh
    }

    fun replaceChars(alphabet: List<CharacterMap>, sequence: CharSequence?, direction: ReplaceDirection, ignoreCase: Boolean): CharSequence? {
        if (sequence != null) {
            val source = CharArray(sequence.length)
            for (i in 0 until sequence.length) {
                val ch = sequence[i]
                val replacement = if (direction == ReplaceDirection.LEFT_RIGHT) findCharByLeft(alphabet, ch, ignoreCase) else findCharByRight(alphabet, ch, ignoreCase)
                source[i] = replacement ?: ch
            }
            return String(source)
        }
        return null
    }

    fun appendOrReplaceChar(source: CharSequence, what: Char?, to: String?, ignoreCase: Boolean, appendOrReplace: Boolean): String {
        if (isEmpty(source) || isEmpty(to)) {
            return EMPTY_STRING
        }
        val newStr = StringBuilder()
        for (i in 0 until source.length) {
            val c = source[i]
            if (charsEqual(c, what, ignoreCase)) {
                if (appendOrReplace) {
                    newStr.append(c)
                    newStr.append(to)
                } else {
                    newStr.append(to)
                }
            } else {
                newStr.append(c)
            }
        }
        return newStr.toString()
    }

    fun replaceByLatinCyrillicAlphabet(sequence: CharSequence, ignoreCase: Boolean, direction: ReplaceDirection): CharSequence? {
        return replaceChars(replaceLatinCyrillicAlphabet, sequence, direction, ignoreCase)
    }

    fun lowerCaseAlphabet(alphabet: List<CharacterMap?>): List<CharacterMap> {
        val newAlphabet: MutableList<CharacterMap> = ArrayList()
        for (map in alphabet) {
            if (map != null) {
                newAlphabet.add(CharacterMap(if (map.leftCh != null) Character.toLowerCase(map.leftCh) else null, if (map.rightCh != null) Character.toLowerCase(map.rightCh) else null))
            }
        }
        return newAlphabet
    }

    fun upperCaseAlphabet(alphabet: List<CharacterMap?>): List<CharacterMap> {
        val newAlphabet: MutableList<CharacterMap> = ArrayList()
        for (map in alphabet) {
            if (map != null) {
                newAlphabet.add(CharacterMap(if (map.leftCh != null) Character.toUpperCase(map.leftCh) else null, if (map.rightCh != null) Character.toUpperCase(map.rightCh) else null))
            }
        }
        return newAlphabet
    }
}

data class CharacterMap(val leftCh: Char?, val rightCh: Char?)

enum class ReplaceDirection {
    LEFT_RIGHT, RIGHT_LEFT
}
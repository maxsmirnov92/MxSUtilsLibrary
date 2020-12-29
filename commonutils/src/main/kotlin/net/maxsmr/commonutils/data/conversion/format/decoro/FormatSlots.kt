package net.maxsmr.commonutils.data.conversion.format.decoro

import ru.tinkoff.decoro.slots.PredefinedSlots.hardcodedSlot
import ru.tinkoff.decoro.slots.Slot
import ru.tinkoff.decoro.slots.Slot.SlotValidator
import ru.tinkoff.decoro.slots.SlotValidatorSet
import ru.tinkoff.decoro.slots.SlotValidators

@JvmOverloads
fun custom(
        includedChars: Collection<Char> = emptyList(),
        excludedChars: Collection<Char> = emptyList(),
        validateRule: ValidateRule = ValidateRule.ALL,
        rules: Int = Slot.RULES_DEFAULT,
        value: Char? = null
): Slot {
    val validators = mutableListOf<SlotValidator>()
    if (includedChars.isNotEmpty()) {
        validators.add(IncludeValidator(includedChars))
    }
    if (excludedChars.isNotEmpty()) {
        validators.add(ExcludeValidator(excludedChars))
    }
    return custom(validators, validateRule, rules, value)
}

/**
 * Использовать, если нужна комбинация из [validators]
 */
@JvmOverloads
fun custom(
        validators: List<SlotValidator>,
        validateRule: ValidateRule = ValidateRule.ANY,
        rules: Int = Slot.RULES_DEFAULT,
        value: Char? = null
) = Slot(rules, value, SlotValidatorSet.setOf(CombinedValidator(validators, validateRule)))

/**
 * Слот, который может содержать только любую букву (кириллица или латиница) или цифру
 */
@JvmOverloads
fun letterOrDigit(
        supportEnglish: Boolean = true,
        supportRussian: Boolean = true,
        rules: Int = Slot.RULES_DEFAULT,
        excludedChars: Collection<Char> = emptyList()
) = letterDigit(true, supportEnglish, supportRussian, rules, excludedChars)

@JvmOverloads
fun letter(
        supportEnglish: Boolean = true,
        supportRussian: Boolean = true,
        rules: Int = Slot.RULES_DEFAULT,
        excludedChars: Collection<Char> = emptyList()
): Slot = letterDigit(false, supportEnglish, supportRussian, rules, excludedChars)

private fun letterDigit(
        withDigits: Boolean,
        supportEnglish: Boolean = true,
        supportRussian: Boolean = true,
        rules: Int = Slot.RULES_DEFAULT,
        excludedChars: Collection<Char> = emptyList()
): Slot {
    val letterValidator = SlotValidators.LetterValidator(supportEnglish, supportRussian)
    val digitValidator = SlotValidators.DigitValidator()
    val validators: MutableList<SlotValidator> = if (withDigits) mutableListOf(letterValidator, digitValidator) else mutableListOf(letterValidator)
    if (excludedChars.isNotEmpty()) {
        validators.add(ExcludeValidator(excludedChars))
    }
    return Slot(rules, null, SlotValidatorSet.setOf(CombinedValidator(validators, ValidateRule.ANY)))
}

/**
 * Слот с захардкоженным пробелом
 */
fun hardcodedSpaceSlot() =
        hardcodedSlot(' ').withTags(Slot.TAG_DECORATION)

/**
 * Слот с захардкоженным '-'
 */
fun hardcodedHyphenSlot() =
        hardcodedSlot('-').withTags(Slot.TAG_DECORATION)

/**
 * Слот с захардкоженным '*'
 */
fun hardcodedStarSlot() =
        hardcodedSlot('*').withTags(Slot.TAG_DECORATION)

/**
 * Слот с захардкоженным '+'
 */
fun hardcodedPlusSlot() =
        hardcodedSlot('+').withTags(Slot.TAG_DECORATION)

/**
 * Слот с захардкоженным '('
 */
fun hardcodedOpenBracketSlot() =
        hardcodedSlot('(').withTags(Slot.TAG_DECORATION)

/*
* Слот с захардкоженным ')'
*/
fun hardcodedClosedBracketSlot() =
        hardcodedSlot(')').withTags(Slot.TAG_DECORATION)

@JvmOverloads
fun getSlots(base: List<Slot>, count: Int, lastIterationInserts: Int = base.size): List<Slot> {
    require(count > 0) { "count must be more than zero" }
    require(lastIterationInserts in 0..base.size) { "lastIterationInserts must be more than zero" }
    val result = mutableListOf<Slot>()
    for (i in 0 until count) {
        if (i == count - 1) {
            for (j in 0 until lastIterationInserts) {
                result.add(base[j])
            }
        } else {
            base.forEach {
                result.add(it)
            }
        }
    }
    return result
}

/**
 * @return как минимум один слот
 */
fun createAnySlots(mergedChars: List<List<Char>>): MutableList<Slot> {
    val result = mutableListOf<Slot>()
    for (partial in mergedChars) {
        result.add(custom(partial))
    }
    if (result.isEmpty()) {
        result.add(custom())
    }
    return result
}

fun getCharsMerged(strings: List<String>): MutableList<List<Char>> {
    val result = mutableListOf<List<Char>>()
    for (s in strings) {
        val partialList = mutableListOf<Char>()
        for (c in s.toCharArray()) {
            partialList.add(c)
        }
        result.add(partialList)
    }
    return result
}

class RomanDigitValidator : SlotValidator {

    private val allowedChars = arrayOf('x', 'v', 'i')

    override fun validate(value: Char): Boolean {
        return allowedChars.any { it.equals(value, true) }
    }
}

class IncludeValidator(val chars: Collection<Char>) : SlotValidator {

    override fun validate(value: Char): Boolean = chars.isEmpty() || chars.contains(value)
}

class ExcludeValidator(val chars: Collection<Char>) : SlotValidator {

    override fun validate(value: Char): Boolean = !chars.contains(value)
}

/**
 * Комбинированный валидатор из нескольких:
 * срабатывает на всех или хотя бы на одном в зав-ти от [validateRule]
 */
private class CombinedValidator(
        private val validators: List<SlotValidator>,
        private val validateRule: ValidateRule = ValidateRule.ALL
) : SlotValidator {

    override fun validate(value: Char) = if (validators.isNotEmpty()) {
        if (validateRule == ValidateRule.ANY) {
            validators.any { it.validate(value) }
        } else {
            validators.all { it.validate(value) }
        }
    } else {
        true
    }
}
enum class ValidateRule {

    ALL, ANY
}
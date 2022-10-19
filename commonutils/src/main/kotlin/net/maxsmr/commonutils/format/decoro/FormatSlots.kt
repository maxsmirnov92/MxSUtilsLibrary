package net.maxsmr.commonutils.format.decoro

import ru.tinkoff.decoro.slots.PredefinedSlots.hardcodedSlot
import ru.tinkoff.decoro.slots.Slot
import ru.tinkoff.decoro.slots.Slot.SlotValidator
import ru.tinkoff.decoro.slots.SlotValidatorSet
import ru.tinkoff.decoro.slots.SlotValidators

@JvmOverloads
fun Set<Char>.toSlot(
    rules: Int = Slot.RULES_DEFAULT,
    value: Char? = null,
): Slot {
    return Slot(rules, value, CharSlotValidator(this).toSet())
}

/**
 * Слот, который может содержать только любую букву (кириллица или латиница) или цифру
 * @param excludedChars символы, которых не должно быть (опционально)
 * @param includedChars символы, которые могут быть в дополнении к letter или digit (опционально)
 * @param ignoreCase включаемые/исключаемые символы с игнорированием регистра
 */
@JvmOverloads
fun letterOrDigit(
    supportEnglish: Boolean = true,
    supportRussian: Boolean = true,
    rules: Int = Slot.RULES_DEFAULT,
    excludedChars: Set<Char> = emptySet(),
    includedChars: Set<Char> = emptySet(),
    ignoreCase: Boolean = true
): Slot {
    var validators = SlotValidatorSet.setOf(
        SlotValidators.LetterValidator(supportEnglish, supportRussian),
        SlotValidators.DigitValidator()
    )
    if (includedChars.isNotEmpty()) {
        validators = SlotValidatorSet.setOf(validators, CharSlotValidator(includedChars, ignoreCase))
    }
    if (excludedChars.isNotEmpty()) {
        validators = validators.exclude(excludedChars, ignoreCase)
    }
    return Slot(rules, null, validators)
}

@JvmOverloads
fun letter(
    supportEnglish: Boolean = true,
    supportRussian: Boolean = true,
    rules: Int = Slot.RULES_DEFAULT,
    excludedChars: Set<Char> = emptySet(),
    ignoreCase: Boolean = true
): Slot = Slot(rules, null, SlotValidators.LetterValidator(supportEnglish, supportRussian).exclude(excludedChars, ignoreCase))

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

/**
 * Слот с захардкоженным ')'
 */
fun hardcodedClosedBracketSlot() =
    hardcodedSlot(')').withTags(Slot.TAG_DECORATION)

fun romanDigitValidator() = CharSlotValidator(setOf('x', 'v', 'i'))

fun SlotValidator.toSet(): SlotValidatorSet =
    this as? SlotValidatorSet ?: SlotValidatorSet.setOf(this)


/**
 * Исключение указанных [chars] в дополнение к имеющимся валидаторам
 */
fun SlotValidator.exclude(chars: Set<Char>, ignoreCase: Boolean): SlotValidatorSet =
    if (chars.isEmpty()) {
        this as? SlotValidatorSet ?: this.toSet()
    } else ExcludeSlotValidator(this, chars, ignoreCase)

class CharSlotValidator @JvmOverloads constructor(
    private val chars: Set<Char>,
    private val ignoreCase: Boolean = true
) : SlotValidator {

    override fun validate(value: Char): Boolean =
        chars.isEmpty() || chars.any { it.equals(value, ignoreCase) }
}

private class ExcludeSlotValidator(
    private val source: SlotValidator,
    private val excludedChars: Set<Char>,
    private val ignoreCase: Boolean = true
) : SlotValidatorSet() {

    override fun validate(value: Char): Boolean =
        source.validate(value) && excludedChars.none { it.equals(value, ignoreCase) }
}
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
 */
@JvmOverloads
fun letterOrDigit(
        supportEnglish: Boolean = true,
        supportRussian: Boolean = true,
        rules: Int = Slot.RULES_DEFAULT,
        excludedChars: Set<Char> = emptySet(),
): Slot = Slot(rules, null,
        SlotValidators.LetterValidator(supportEnglish, supportRussian)
                or SlotValidators.DigitValidator()
                exclude excludedChars
)

@JvmOverloads
fun letter(
        supportEnglish: Boolean = true,
        supportRussian: Boolean = true,
        rules: Int = Slot.RULES_DEFAULT,
        excludedChars: Set<Char> = emptySet(),
): Slot = Slot(rules, null, SlotValidators.LetterValidator(supportEnglish, supportRussian) exclude excludedChars)

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

fun romanDigitValidator() = CharSlotValidator(setOf('x', 'v', 'i'))

fun SlotValidator.toSet(): SlotValidatorSet =
        this as? SlotValidatorSet ?: SlotValidatorSet.setOf(this)

infix fun SlotValidator.or(other: SlotValidator): SlotValidatorSet =
        SlotValidatorSet.setOf(this, other)

infix fun SlotValidator.exclude(chars: Set<Char>): SlotValidatorSet =
        if (chars.isEmpty()) {
            this as? SlotValidatorSet ?: this.toSet()
        } else {
            ExcludeSlotValidator(this, chars)
        }


class CharSlotValidator(
        private val chars: Set<Char>,
) : SlotValidator {

    override fun validate(value: Char): Boolean =
            chars.isEmpty() || chars.any { it.equals(value, true) }
}


class ExcludeSlotValidator(
        private val source: SlotValidator,
        private val excludedChars: Set<Char>,
) : SlotValidatorSet() {

    override fun validate(value: Char): Boolean =
            source.validate(value) && excludedChars.none { it.equals(value, true) }
}
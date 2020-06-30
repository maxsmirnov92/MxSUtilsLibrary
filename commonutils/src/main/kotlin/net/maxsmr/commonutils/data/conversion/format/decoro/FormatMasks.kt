package net.maxsmr.commonutils.data.conversion.format.decoro

private val DIGITS_STAR = listOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*'
)

// region: захардкоженные decoro-маски, используемые в аппе

//### - ### - ### - ##
@JvmField
val MASK_SNILS = listOf(
        letterOrDigit(false),
        letterOrDigit(false),
        letterOrDigit(false),
        hardcodedSpaceSlot(),
        hardcodedHyphenSlot(),
        hardcodedSpaceSlot(),
        letterOrDigit(false),
        letterOrDigit(false),
        letterOrDigit(false),
        hardcodedSpaceSlot(),
        hardcodedHyphenSlot(),
        hardcodedSpaceSlot(),
        letterOrDigit(false),
        letterOrDigit(false),
        letterOrDigit(false),
        hardcodedSpaceSlot(),
        hardcodedHyphenSlot(),
        hardcodedSpaceSlot(),
        letterOrDigit(false),
        letterOrDigit(false)
)

/**
 * нестрогая маска для телефона: допускает "*" после "7" в исходной строке
 */
val MASK_NON_STRICT_PHONE_NUMBER = listOf(
        hardcodedPlusSlot(),
        any(listOf('7')),
        hardcodedSpaceSlot(),
        hardcodedOpenBracketSlot(),
        any(DIGITS_STAR),
        any(DIGITS_STAR),
        any(DIGITS_STAR),
        hardcodedClosedBracketSlot(),
        hardcodedSpaceSlot(),
        any(DIGITS_STAR),
        any(DIGITS_STAR),
        any(DIGITS_STAR),
        hardcodedSpaceSlot(),
        any(DIGITS_STAR),
        any(DIGITS_STAR),
        hardcodedSpaceSlot(),
        any(DIGITS_STAR),
        any(DIGITS_STAR)
)

// endregion
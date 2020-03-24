package net.maxsmr.commonutils.data.conversion.format

import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.widget.EditText
import android.widget.TextView
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.validation.isRusPhoneNumberValid
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

const val DEFAULT_RUS_PHONE_MASK = "+7 (___) ___ __ __"
const val DEFAULT_PHONE_LENGTH = 12

val DEFAULT_RUS_PHONE_MASK_IMPL: MaskImpl = createPhoneMask()
private val REG_EX_PHONE_MASK: Regex = Regex("([0-9]|\\+)")

@JvmOverloads
fun normalizePhoneNumber(phoneNumber: CharSequence, prefixReplaceWith: String = "+7"): String {
    if (phoneNumber.isEmpty()) return EMPTY_STRING
    var prefixReplaceWith = prefixReplaceWith
    var normalized = phoneNumber.replace("[^0-9+]".toRegex(), EMPTY_STRING)
    val seven = "7"
    val plusSeven = "+7"
    val eight = "8"
    val nine = "9"
    val replaceSubstring = when {
        seven != prefixReplaceWith && normalized.startsWith(seven) -> {
            seven
        }
        plusSeven != prefixReplaceWith && normalized.startsWith(plusSeven) -> {
            plusSeven
        }
        eight != prefixReplaceWith && normalized.startsWith(eight) -> {
            eight
        }
        nine != prefixReplaceWith && normalized.startsWith(nine) -> {
            nine
        }
        else -> {
            EMPTY_STRING
        }
    }
    if (replaceSubstring == nine) {
        prefixReplaceWith += nine
    }
    if (replaceSubstring.isNotEmpty()) {
        normalized = normalized.replaceFirst(replaceSubstring, prefixReplaceWith)
    }
    return normalized
}

fun normalizePhoneNumberRemovePlus(phoneNumber: CharSequence): String =
        normalizePhoneNumber(phoneNumber).trim('+')

/**
 * Убрать символы, которые несовместимы
 * с inputType phone
 */
fun clearPhone(phoneNumber: String) = phoneNumber.replace("-", " ").replace("*", EMPTY_STRING)

fun formatPhoneNumber(
        phoneNumber: String,
        withMask: Boolean = false,
        rangeToMask: IntRange = IntRange(8, 10)
): String {
    var phoneFormatted = normalizePhoneNumber(phoneNumber)

    return if (phoneFormatted.startsWith("+7") && phoneFormatted.length == DEFAULT_PHONE_LENGTH) {
        if (withMask) {
            phoneFormatted = phoneFormatted.replaceRange(rangeToMask, "*".repeat(rangeToMask.last - rangeToMask.first + 1))
        }
        "${phoneFormatted.substring(0, 2)} " +
                "(${phoneFormatted.substring(2, 5)}) " +
                "${phoneFormatted.substring(5, 8)} " +
                "${phoneFormatted.substring(8, 10)} " +
                phoneFormatted.substring(10, 12)
    } else {
        PhoneNumberUtils.formatNumber(phoneFormatted)
    }
}

/**
 * Может быть использован в afterTextChanges в динамике;
 * в активном [watcher] меняются маски в зав-ти от условий
 */
fun EditText.formatPhone(
        current: Editable?,
        watcher: MaskFormatWatcher,
        mask: MaskImpl = DEFAULT_RUS_PHONE_MASK_IMPL
) {
    // watcher переиспользуется
    current?.let {
        if (current.length == 1 && current[0].toString().matches(REG_EX_PHONE_MASK)) {
            setPhoneHead(current)
            applyMask(this.text, watcher, mask)
        } else if (current.isEmpty()) {
            EMPTY_MASK.clear()
            watcher.setMask(EMPTY_MASK)
        } else {
            // do nothing
        }
    }
}

fun EditText.setPhoneHead(editable: Editable) {
    if (editable[0] in hashSetOf('+', '7', '8')) {
        this.text.clear()
        this.append("+7")
    } else {
        val text = "+7${editable[0]}"
        this.text.clear()
        this.append(text)
    }
}

fun TextView.setPhoneFormattedText(
        text: CharSequence,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true
) = setFormattedText(text, DEFAULT_RUS_PHONE_MASK_IMPL, applyWatcher, isDistinct) {
    isRusPhoneNumberValid(it)
}

fun createPhoneMask(phoneMask: String = DEFAULT_RUS_PHONE_MASK): MaskImpl {
    val slots = UnderscoreDigitSlotsParser().parseSlots(phoneMask)
    return MaskImpl.createTerminated(slots)
}

fun createPhoneWatcher(phoneMask: String = DEFAULT_RUS_PHONE_MASK): MaskFormatWatcher {
    return MaskFormatWatcher(createPhoneMask(phoneMask))
}
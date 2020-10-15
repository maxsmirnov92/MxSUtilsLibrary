package net.maxsmr.commonutils.data.conversion.format

import android.widget.TextView
import net.maxsmr.commonutils.android.gui.setTextChecked
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.slots.Slot
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

val EMPTY_MASK = createEmptyMask()

/**
 * Убрать из строки перечисленные символы
 */
fun clearText(text: CharSequence, excludedChars: Collection<Char>): String {
    var result = text
    excludedChars.forEach {
        result = result.replace(it.toString().toRegex(), "")
    }
    return result.toString()
}

/**
 * @return неотформатированный [text] по исходной [mask]
 */
fun clearText(mask: MaskImpl, text: CharSequence): String {
    val copyMask = MaskImpl(mask)
    copyMask.clear()
    copyMask.insertFront(text)
    return copyMask.toUnformattedString()
}

/**
 * Убрать из строки такое кол-во символов, не соответствующих [limitedChars], чтобы общий размер не превышал [targetTextSize]
 * @param checkClearedTextSize необходимость проверки строки после преобразований на соответствие [targetTextSize] (т.к. могло быть превышено за счёт незапретных символов)
 */
@JvmOverloads
fun clearText(
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
    val targetLimitedCharsCount = if (limitedCharsCount > removedCharsCount) limitedCharsCount - removedCharsCount else 0
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
    require(size > 0) {"Incorrect size: $size"}
    val result = StringBuilder()
    for (i in 0 until size) {
        result.append(placeholderChar)
    }
    return result.toString()
}

/**
 * Проверить строку на наличие маскируемых цифр
 */
fun containsMasked(text: CharSequence) = if (text.contains("*")) EMPTY_STRING else text

/**
 * @param applyWatcher применять на постоянной основе или одноразовое форматирование
 * @param isDistinct перед выставлением окончательного результата проверять на уникальность, чтобы не сбивался курор
 */
@JvmOverloads
fun TextView.setFormattedText(
        text: CharSequence,
        mask: MaskImpl,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true,
        textValidator: ((String) -> Boolean)? = null
): MaskFormatWatcher? {
    this.text = text
    val currentText = this.text?.toString() ?: EMPTY_STRING
    if (textValidator == null || textValidator(currentText)) {
        val watcher =
                if (applyWatcher) {
                    // watcher пересоздаётся
                    MaskFormatWatcher(mask).apply {
                        installOn(this@setFormattedText)
                    }
                } else {
                    null
                }
        val newText = formatText(currentText,
                mask,
                watcher)
        setTextChecked(newText, isDistinct)
        return watcher
    }
    return null
}

@JvmOverloads
fun applyToMask(
        text: CharSequence,
        mask: MaskImpl,
        watcher: MaskFormatWatcher? = null
): MaskImpl {
    mask.clear()
    mask.isHideHardcodedHead = true
    mask.insertFront(text.toString())
    watcher?.setMask(mask)
    return mask
}

@JvmOverloads
fun formatText(
        text: CharSequence,
        mask: MaskImpl,
        watcher: MaskFormatWatcher? = null
) = applyToMask(text, mask, watcher).toString()

@JvmOverloads
fun createMask(slots: Collection<Slot>, isTerminated: Boolean = true) =
        with(slots.toTypedArray()) {
            if (isTerminated) MaskImpl.createTerminated(this) else MaskImpl.createNonTerminated(this)
        }

/**
 * @return маска, в которой "_" будет заменено на цифру
 */
@JvmOverloads
fun createDigitsMask(
        pattern: String,
        isTerminated: Boolean = true,
        hideHardcodedHead: Boolean = false
): MaskImpl {
    val slots = UnderscoreDigitSlotsParser().parseSlots(pattern)
    return if (isTerminated) MaskImpl.createTerminated(slots) else MaskImpl.createNonTerminated(slots).apply {
        isHideHardcodedHead = hideHardcodedHead
    }
}

@JvmOverloads
fun createDigitsWatcher(
        pattern: String,
        isTerminated: Boolean = true,
        hideHardcodedHead: Boolean = false
): MaskFormatWatcher =
        MaskFormatWatcher(createDigitsMask(pattern, isTerminated, hideHardcodedHead))

private fun createEmptyMask(): MaskImpl {
    return MaskImpl.createNonTerminated(arrayOf(PredefinedSlots.any()))
}
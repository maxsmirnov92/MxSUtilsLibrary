package net.maxsmr.commonutils.data.conversion.format

import android.widget.TextView
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.validation.isValidRusNumber
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

val EMPTY_MASK = createEmptyMask()

/**
 * Проверить строку на наличие маскируемых цифр
 */
fun containsMasked(text: CharSequence) = if (text.contains("*")) EMPTY_STRING else text

/**
 * @param applyWatcher применять на постоянной основе или одноразовое форматирование
 * @param isDistinct перед выставлением окончательного результата проверять на уникальность, чтобы не сбивался курор
 */
fun TextView.setFormattedText(
        text: CharSequence,
        mask: MaskImpl,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true
): MaskFormatWatcher? {
    this.text = text
    val currentText = this.text?.toString() ?: EMPTY_STRING
    if (isValidRusNumber(currentText)) {
        val watcher =
                if (applyWatcher) {
                    // watcher пересоздаётся
                    MaskFormatWatcher(mask).apply {
                        installOn(this@setFormattedText)
                    }
                } else {
                    null
                }
        val newText = applyMask(currentText,
                watcher,
                mask).toString()
        if (!isDistinct || newText != this.text) {
            this.text = newText
        }
        return watcher
    }
    return null
}

fun applyMask(
        text: CharSequence,
        watcher: MaskFormatWatcher?,
        mask: MaskImpl
): MaskImpl {
    mask.clear()
    mask.isHideHardcodedHead = true
    mask.insertFront(text.toString())
    watcher?.setMask(mask)
    return mask
}

fun createEmptyMask(): MaskImpl {
    return MaskImpl.createNonTerminated(arrayOf(PredefinedSlots.any()))
}
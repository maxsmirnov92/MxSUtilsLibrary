package net.maxsmr.commonutils.format

import android.widget.TextView
import net.maxsmr.commonutils.gui.setTextDistinct
import net.maxsmr.commonutils.text.EMPTY_STRING
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.slots.PredefinedSlots
import ru.tinkoff.decoro.slots.Slot
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

val EMPTY_MASK = createEmptyMask()

fun getFormattedText(mask: Mask, text: CharSequence?): String =
        getText(mask, text, false)

fun getUnformattedText(mask: Mask, text: CharSequence?): String =
        getText(mask, text, true)

private fun getText(mask: Mask, text: CharSequence?, isUnformatted: Boolean): String {
    val copyMask: Mask = if (mask is MaskImpl) {
        MaskImpl(mask).apply {
            clear()
        }
    } else {
        mask
    }
    try {
        copyMask.insertFront(text)
    } catch (e: UnsupportedOperationException) {
        return EMPTY_STRING
    }
    return if (isUnformatted) copyMask.toUnformattedString() else copyMask.toString()
}

/**
 * @param applyWatcher применять на постоянной основе или одноразовое форматирование
 * @param isDistinct перед выставлением окончательного результата проверять на уникальность, чтобы не сбивался курсор
 */
@JvmOverloads
fun TextView.setFormattedText(
        text: CharSequence,
        mask: MaskImpl,
        prefix: String = EMPTY_STRING,
        installOnAndFill: Boolean = false,
        applyWatcher: Boolean = true,
        isDistinct: Boolean = true,
        textValidator: ((String) -> Boolean)? = null
): MaskFormatWatcher? {
    this.text = text
    val currentText = this.text?.toString() ?: EMPTY_STRING
    if (textValidator == null || textValidator(currentText)) {
        val watcher = if (applyWatcher) {
            // watcher пересоздаётся
            createWatcher(mask).apply {
                if (!installOnAndFill) {
                    installOn(this@setFormattedText)
                } else {
                    installOnAndFill(this@setFormattedText)
                }
                if (prefix.isNotEmpty()) {
                    this@setFormattedText.text = prefix
                }
            }
        } else {
            null
        }
        val newText = formatText(currentText,
                mask,
                watcher)
        if (isDistinct) {
            setTextDistinct(newText)
        } else {
            this.text = newText
        }
        return watcher
    }
    return null
}

@JvmOverloads
fun applyToMask(
        text: CharSequence,
        mask: MaskImpl,
        watcher: MaskFormatWatcher? = null,
        hideHardcodedHead: Boolean = true
): MaskImpl {
    mask.clear()
    mask.isHideHardcodedHead = hideHardcodedHead
    mask.insertFront(text.toString())
    watcher?.setMask(mask)
    return mask
}

@JvmOverloads
fun formatText(
        text: CharSequence,
        mask: MaskImpl,
        watcher: MaskFormatWatcher? = null,
) = applyToMask(text, mask, watcher).toString()

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
    return createMask(slots.toList(), isTerminated, hideHardcodedHead)
}

@JvmOverloads
fun createDigitsWatcher(
        pattern: String,
        isTerminated: Boolean = true,
        hideHardcodedHead: Boolean = false,
        maskConfigurator: ((MaskImpl) -> Unit)? = null
): MaskFormatWatcher =
        createWatcher(createDigitsMask(pattern, isTerminated, hideHardcodedHead), maskConfigurator)

@JvmOverloads
fun createMask(
        slots: Collection<Slot>,
        isTerminated: Boolean = true,
        hideHardcodedHead: Boolean = false,
) = with(slots.toTypedArray()) {
    if (isTerminated) MaskImpl.createTerminated(this) else MaskImpl.createNonTerminated(this).apply {
        isHideHardcodedHead = hideHardcodedHead
    }
}

@JvmOverloads
fun createWatcher(
        slots: Collection<Slot>,
        isTerminated: Boolean = true,
        hideHardcodedHead: Boolean = false,
        maskConfigurator: ((MaskImpl) -> Unit)? = null
): MaskFormatWatcher = createWatcher(createMask(slots, isTerminated, hideHardcodedHead), maskConfigurator)


@JvmOverloads
fun createWatcher(mask: MaskImpl, maskConfigurator: ((MaskImpl) -> Unit)? = null): MaskFormatWatcher =
        MaskFormatWatcher(mask.apply {
            maskConfigurator?.invoke(this)
        })

private fun createEmptyMask(): MaskImpl =
        createMask(listOf(PredefinedSlots.any()), false, true)
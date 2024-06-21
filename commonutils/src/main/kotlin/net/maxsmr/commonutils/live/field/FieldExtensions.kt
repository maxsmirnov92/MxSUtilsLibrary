package net.maxsmr.commonutils.live.field

import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.format.getFormattedText
import net.maxsmr.commonutils.gui.setSelectionToEnd
import net.maxsmr.commonutils.gui.setTextDistinct
import net.maxsmr.commonutils.gui.setTextDistinctFormatted
import ru.tinkoff.decoro.Mask
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

/**
 * Обозревание форматированного или неформатированного (второй вариант более правильный)
 * из [Field] с целью выставления в целевую [view] форматированного значения методом refreshMask
 * @param maskWatcher уже привязанный к [view]
 */
@JvmOverloads
fun Field<String>.observeFromTextFormatted(
        view: TextView,
        owner: LifecycleOwner,
        maskWatcher: MaskFormatWatcher,
        asString: Boolean = true,
        onChanged: ((String?) -> Unit)? = null
) {
    valueLive.observe(owner) {
        onChanged?.invoke(it)
        if (view.setTextDistinctFormatted(it, maskWatcher, asString)) {
            (view as? EditText)?.setSelectionToEnd()
        }
    }
}

@JvmOverloads
fun Field<String>.observeFromTextFormatted(
        view: TextView,
        owner: LifecycleOwner,
        mask: Mask,
        asString: Boolean = true,
        onChanged: ((String?) -> Unit)? = null
) {
    observeFromText(view, owner, asString) {
        onChanged?.invoke(it)
        getFormattedText(mask, it)
    }
}

/**
 * Обозревание форматированного или неформатированного (второй вариант более правильный)
 * из [Field] с целью выставления в целевую [view] форматированного значения через [formatFunc]
 */
@JvmOverloads
fun Field<String>.observeFromText(
        view: TextView,
        owner: LifecycleOwner,
        asString: Boolean = true,
        formatFunc: ((String) -> CharSequence?)? = null
) {
    observeFrom(view, owner, asString) {
        formatFunc?.invoke(it) ?: it
    }
}

@JvmOverloads
fun <D> Field<D>.observeFrom(
        view: TextView,
        owner: LifecycleOwner,
        asString: Boolean = true,
        formatFunc: (D) -> CharSequence?
) {
    valueLive.observe(owner) {
        if (view.setTextDistinct(formatFunc(it), asString)) {
            (view as? EditText)?.setSelectionToEnd()
        }
    }
}

fun <D> Field<D>.clearErrorOnChange(lifecycleOwner: LifecycleOwner, onChanged: ((D) -> Unit)? = null) {
    valueLive.observe(lifecycleOwner) {
        onChanged?.invoke(it)
        clearError()
    }
}

/**
 * @return null, если все обязательные
 * (или необязательные при непустом значении в зав-ти от [ifEmpty])
 * филды прошли валидацию,
 * или еррорный филд
 */
@JvmOverloads
fun Collection<Field<*>>.validateAndSetByRequiredFirstField(ifEmpty: Boolean = true): Field<*>? {
    return validateAndSetByFirst { it.validateAndSetByRequired(ifEmpty) }
}

@JvmOverloads
fun Collection<Field<*>>.validateAndSetByRequiredFields(ifEmpty: Boolean = true): List<Field<*>> {
    return validateAndSet { it.validateAndSetByRequired(ifEmpty) }
}

@JvmOverloads
fun Collection<Field<*>>.validateAndSetByRequiredFirst(ifEmpty: Boolean = true): Boolean {
    return validateAndSetByRequiredFirstField(ifEmpty) == null
}

@JvmOverloads
fun Collection<Field<*>>.validateAndSetByRequired(ifEmpty: Boolean = true): Boolean {
    return validateAndSetByRequiredFields(ifEmpty).isEmpty()
}

fun Collection<Field<*>>.validateAndSetByFirst(predicate: (Field<*>) -> Boolean): Field<*>? {
    var errorField: Field<*>? = null
    forEach {
        if (!predicate(it)) {
            errorField = it
            return@forEach
        }
    }
    return errorField
}

fun Collection<Field<*>>.validateAndSet(predicate: (Field<*>) -> Boolean): List<Field<*>> {
    val errorFields = mutableListOf<Field<*>>()
    forEach {
        if (!predicate(it)) {
            errorFields.add(it)
        }
    }
    return errorFields
}
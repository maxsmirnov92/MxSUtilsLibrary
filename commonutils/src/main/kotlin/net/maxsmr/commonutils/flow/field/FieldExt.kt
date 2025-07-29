package net.maxsmr.commonutils.flow.field

import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.flow.observeLatest
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
    onChanged: (suspend (String?) -> Unit)? = null
) {
    valueFlow.observeLatest(owner) {
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
    valueFlow.observeLatest(owner) {
        if (view.setTextDistinct(formatFunc(it), asString)) {
            (view as? EditText)?.setSelectionToEnd()
        }
    }
    enabledFlow.observeLatest(owner) {
        view.isEnabled = it
    }
}

fun <D> Field<D>.observeWithClearError(
    scope: CoroutineScope,
    onChanged: (suspend (D) -> Unit)? = null
) {
    valueFlow.observeLatest(scope) {
        onChanged?.invoke(it)
        clearError()
    }
}

/**
 * @return null, если все обязательные
 * (или необязательные при непустом значении в зав-ти от [ifEmpty])
 * филды прошли валидацию, или первый еррорный филд
 */
@JvmOverloads
fun Collection<Field<*>>.validateAndSetByRequiredFirst(ifEmpty: Boolean = true): Field<*>? {
    return firstOrNull { !it.validateAndSetByRequired(ifEmpty) }
}

@JvmOverloads
fun Collection<Field<*>>.validateAndSetByRequired(ifEmpty: Boolean = true): List<Field<*>> {
    return filter { !it.validateAndSetByRequired(ifEmpty) }
}

fun List<Field<*>>.anyRequiredFieldIsEmptyFlow(): Flow<Boolean> {
    return combine(this.map { f ->
        combine(f.isEmptyFlow, f.requiredFlow) { isEmpty, required ->
            isEmpty to required
        }.map { (isEmpty, required) ->
            if (required) {
                isEmpty
            } else {
                false
            }
        }
    }) { array ->
        array.any { it }
    }
}

fun List<Field<*>>.anyRequiredFieldHasErrorFlow(): Flow<Boolean> {
    return combine(this.map { f ->
        combine(f.errorFlow, f.requiredFlow) { error, required ->
            error to required
        }.map { (error, required) ->
            if (required) {
                error
            } else {
                null
            }
        }
    }) { array ->
        array.any { it != null }
    }
}

fun List<Field<*>>.anyRequiredFieldIsEmptyOrErrorFlow(): Flow<Boolean> {
    return combine(this.map { f ->
        combine(f.isEmptyFlow, f.errorFlow, f.requiredFlow) { isEmpty, error, required ->
            Triple(error, isEmpty, required)
        }.map { (error, isEmpty, required) ->
            if (required) {
                error != null || isEmpty
            } else {
                false
            }
        }
    }) { array ->
        array.any { it }
    }
}
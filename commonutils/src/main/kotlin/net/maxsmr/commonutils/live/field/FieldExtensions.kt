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

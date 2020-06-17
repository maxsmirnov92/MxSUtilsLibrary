package net.maxsmr.commonutils.android.gui.fragments.actions.message

import android.content.Context
import net.maxsmr.commonutils.android.gui.fragments.actions.BaseViewModelAction

/**
 * Базовое действие для показа сообщения
 */
abstract class BaseMessageAction<T>: BaseViewModelAction<Context>() {

    open var tag: Any? = null

    var show: Boolean = true

    private var lastShowed: T? = null

    protected abstract fun show(context: Context): T

    protected abstract fun hide(message: T)

    override fun doAction(actor: Context) {
        // в большинстве случаев бесполезно, т.к. каждый раз новый Action
        lastShowed?.let {
            hide(it)
            lastShowed = null
        }
        if (show) {
            lastShowed = show(actor)
        }
        super.doAction(actor)
    }
}
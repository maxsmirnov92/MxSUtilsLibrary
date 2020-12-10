package net.maxsmr.commonutils.android.gui.actions.message

import net.maxsmr.commonutils.android.gui.actions.BaseViewModelAction

/**
 * Базовое действие для показа сообщения
 */
abstract class BaseMessageAction<T, Actor>: BaseViewModelAction<Actor>() {

    open var tag: Any? = null

    var show: Boolean = true

    private var lastShowed: T? = null

    protected abstract fun show(actor: Actor): T

    protected abstract fun hide(message: T)

    override fun doAction(actor: Actor) {
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
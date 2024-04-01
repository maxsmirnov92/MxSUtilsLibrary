package net.maxsmr.rx_extensions.actions.message

import net.maxsmr.rx_extensions.actions.BaseViewModelAction

/**
 * Базовое действие для показа сообщения
 */
abstract class BaseMessageAction<T, Actor> : BaseViewModelAction<Actor>() {

    var show: Boolean = true

    var lastShowed: T? = null

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
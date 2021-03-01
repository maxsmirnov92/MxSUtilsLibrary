package net.maxsmr.commonutils.gui.actions.message

open class AnyMessageAction<T, Actor>(
        val message: T,
        val showFunc: (Actor, T) -> Unit,
        val hideFunc: (T) -> Unit,
): BaseMessageAction<T, Actor>() {

    override fun show(actor: Actor): T {
        showFunc(actor, message)
        return message
    }

    override fun hide(message: T) {
        hideFunc(message)
    }
}
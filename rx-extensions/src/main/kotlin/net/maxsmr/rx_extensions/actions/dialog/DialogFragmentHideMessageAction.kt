package net.maxsmr.rx_extensions.extensions.actions.dialog

import net.maxsmr.rx_extensions.actions.BaseTaggedViewModelAction
import net.maxsmr.rx_extensions.dialogs.holder.DialogFragmentsHolder

data class DialogFragmentHideMessageAction(
        override val tag: String
) : BaseTaggedViewModelAction<DialogFragmentsHolder>() {

    override fun doAction(actor: DialogFragmentsHolder) {
        actor.hide(tag)
        super.doAction(actor)
    }
}
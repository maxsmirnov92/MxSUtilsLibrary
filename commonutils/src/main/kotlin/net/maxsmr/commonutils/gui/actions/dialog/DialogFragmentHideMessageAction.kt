package net.maxsmr.commonutils.gui.actions.dialog

import net.maxsmr.commonutils.gui.actions.BaseTaggedViewModelAction
import net.maxsmr.commonutils.gui.fragments.dialogs.holder.DialogFragmentsHolder

data class DialogFragmentHideMessageAction(
        override val tag: String
) : BaseTaggedViewModelAction<DialogFragmentsHolder>() {

    override fun doAction(actor: DialogFragmentsHolder) {
        actor.hide(tag)
        super.doAction(actor)
    }
}
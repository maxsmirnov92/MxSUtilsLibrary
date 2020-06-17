package net.maxsmr.commonutils.android.gui.fragments.actions.dialog

import net.maxsmr.commonutils.android.gui.fragments.actions.BaseTaggedViewModelAction
import net.maxsmr.commonutils.android.gui.fragments.dialogs.holder.DialogFragmentsHolder


data class DialogFragmentHideMessageAction(
        override val tag: String
) : BaseTaggedViewModelAction<DialogFragmentsHolder>() {

    override fun doAction(actor: DialogFragmentsHolder) {
        actor.hide(tag)
        super.doAction(actor)
    }
}
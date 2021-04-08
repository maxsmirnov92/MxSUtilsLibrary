package net.maxsmr.commonutils.gui.actions.dialog

import android.app.Dialog
import net.maxsmr.commonutils.gui.actions.BaseTaggedViewModelAction
import net.maxsmr.commonutils.gui.fragments.dialogs.BaseTypedDialogFragment
import net.maxsmr.commonutils.gui.fragments.dialogs.holder.DialogFragmentsHolder

data class DialogBuilderFragmentShowMessageAction<D: Dialog, F : BaseTypedDialogFragment<D>>(
        override val tag: String,
        val builder: BaseTypedDialogFragment.Builder<D, F>,
        val reshow: Boolean = true
) : BaseTaggedViewModelAction<DialogFragmentsHolder>() {

    override fun doAction(actor: DialogFragmentsHolder) {
        actor.show(tag, builder.build(actor.currentContext ?: throw IllegalStateException("context is not attached to DialogFragmentsHolder")), reshow)
        super.doAction(actor)
    }
}
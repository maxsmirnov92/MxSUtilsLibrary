package net.maxsmr.rx_extensions.extensions.actions.dialog

import android.app.Dialog
import net.maxsmr.rx_extensions.actions.BaseTaggedViewModelAction
import net.maxsmr.rx_extensions.dialogs.BaseTypedDialogFragment
import net.maxsmr.rx_extensions.dialogs.holder.DialogFragmentsHolder

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
package net.maxsmr.rx_extensions.extensions.actions.dialog

import androidx.fragment.app.DialogFragment
import net.maxsmr.rx_extensions.actions.BaseTaggedViewModelAction
import net.maxsmr.rx_extensions.dialogs.holder.DialogFragmentsHolder

@Deprecated("creating DialogFragment on VM is not recommended", replaceWith = ReplaceWith("use DialogBuilderFragmentShowMessageAction") )
data class DialogFragmentShowMessageAction(
        override val tag: String,
        val fragment: DialogFragment,
        val reshow: Boolean = true
) : BaseTaggedViewModelAction<DialogFragmentsHolder>() {

    override fun doAction(actor: DialogFragmentsHolder) {
        actor.show(tag, fragment, reshow)
        super.doAction(actor)
    }
}
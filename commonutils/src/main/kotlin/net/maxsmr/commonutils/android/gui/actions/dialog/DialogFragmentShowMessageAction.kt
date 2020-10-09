package net.maxsmr.commonutils.android.gui.actions.dialog

import androidx.fragment.app.DialogFragment
import net.maxsmr.commonutils.android.gui.actions.BaseTaggedViewModelAction
import net.maxsmr.commonutils.android.gui.fragments.dialogs.TypedDialogFragment
import net.maxsmr.commonutils.android.gui.fragments.dialogs.holder.DialogFragmentsHolder

data class DialogFragmentShowMessageAction(
        override val tag: String,
        val fragment: DialogFragment,
        val reshow: Boolean = true
) : BaseTaggedViewModelAction<DialogFragmentsHolder>() {

    constructor(
            tag: String,
            builder: TypedDialogFragment.Builder<TypedDialogFragment<*>>,
            reshow: Boolean = true
    ) : this(tag, builder.build(), reshow)

    override fun doAction(actor: DialogFragmentsHolder) {
        actor.show(tag, fragment, reshow)
        super.doAction(actor)
    }
}
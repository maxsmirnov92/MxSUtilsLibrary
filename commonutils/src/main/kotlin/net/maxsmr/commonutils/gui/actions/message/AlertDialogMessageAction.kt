package net.maxsmr.commonutils.gui.actions.message

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

@Deprecated(message = "", replaceWith = ReplaceWith("DialogBuilderFragmentShowMessageAction"))
data class AlertDialogMessageAction(
        @StringRes
        val titleResId: Int = 0,
        val title: CharSequence? = null,
        @StringRes
        val messageResId: Int = 0,
        val message: CharSequence? = null,
        val isCancelable: Boolean = false,
        @StringRes
        val positiveClickTextResId: Int = 0,
        @StringRes
        val neutralClickTextResId: Int = 0,
        @StringRes
        val negativeClickTextResId: Int = 0,
        val positiveClickText: CharSequence? = null,
        val neutralClickText: CharSequence? = null,
        val negativeClickText: CharSequence? = null,
        val positiveClickListener: (() -> Unit)? = null,
        val neutralClickListener: (() -> Unit)? = null,
        val negativeClickListener: (() -> Unit)? = null
) : BaseMessageAction<DialogInterface, Context>() {

    override fun show(actor: Context): DialogInterface {
        val builder = AlertDialog.Builder(actor)
        val title = if (titleResId != 0) {
            actor.getString(titleResId)
        } else {
            title
        }
        if (title != null) {
            builder.setTitle(title)
        }
        val message = if (messageResId != 0) {
            actor.getString(messageResId)
        } else {
            message
        }
        if (message != null) {
            builder.setMessage(message)
        }
        builder.setCancelable(isCancelable)

        val positiveClickText = if (positiveClickTextResId != 0) {
            actor.getString(positiveClickTextResId)
        } else {
            positiveClickText
        }
        val neutralClickText = if (neutralClickTextResId != 0) {
            actor.getString(neutralClickTextResId)
        } else {
            neutralClickText
        }
        val negativeClickText = if (negativeClickTextResId != 0) {
            actor.getString(negativeClickTextResId)
        } else {
            negativeClickText
        }
        if (positiveClickText != null) {
            builder.setPositiveButton(positiveClickText) { dialog, which -> positiveClickListener?.invoke() }
        }
        if (neutralClickText != null) {
            builder.setPositiveButton(neutralClickText) { dialog, which -> positiveClickListener?.invoke() }
        }
        if (negativeClickText != null) {
            builder.setPositiveButton(negativeClickText) { dialog, which -> positiveClickListener?.invoke() }
        }
        return builder.show()
    }

    override fun hide(message: DialogInterface) {
        message.dismiss()
    }
}
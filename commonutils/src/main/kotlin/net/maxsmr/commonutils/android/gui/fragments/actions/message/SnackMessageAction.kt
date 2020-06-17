package net.maxsmr.commonutils.android.gui.fragments.actions.message

import android.content.Context
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.commonutils.data.text.EMPTY_STRING

data class SnackMessageAction(
        @StringRes
        val messageResId: Int = 0,
        val message: CharSequence = EMPTY_STRING,
        @ColorRes
        val backgroundColorResId: Int = 0,
        @ColorInt
        val backgroundColor: Int? = null,
        @StringRes
        val actionResId: Int = 0,
        val action: CharSequence? = null,
        @ColorRes
        val actionColorResId: Int = 0,
        val actionColor: Int? = null,
        val actionListener: ((view: View) -> Unit)? = null,
        val duration: Int = Snackbar.LENGTH_SHORT
): BaseMessageAction<Snackbar>() {

    // выставляем из того места, где action принят
    var view: View? = null

    override fun show(context: Context): Snackbar {
        val view = view
        require (view != null) {
            "Cannot show Snack: view is not specified"
        }
        val message = if (messageResId != 0) {
            context.getString(messageResId)
        } else {
            message
        }
        var duration = duration
        if (duration != Snackbar.LENGTH_SHORT
                && duration != Snackbar.LENGTH_LONG
                && duration != Snackbar.LENGTH_INDEFINITE) {
            duration = Snackbar.LENGTH_SHORT
        }
        Snackbar.make(view, message, duration).apply {
            val backgroundColor: Int? = if (backgroundColorResId != 0) {
                ContextCompat.getColor(context, backgroundColorResId)
            } else {
                backgroundColor
            }
            if (backgroundColor != null) {
                view.setBackgroundColor(backgroundColor)
            }
            val actionText = if (actionResId != 0) {
                context.getString(actionResId)
            } else {
                action
            }
            if (actionText != null) {
                setAction(actionText) { view -> actionListener?.invoke(view) }
            }
            val actionButtonColor = if (actionColorResId != 0) {
                ContextCompat.getColor(context, actionColorResId)
            } else {
                actionColor
            }
            if (actionButtonColor != null) {
                setActionTextColor(actionButtonColor)
            }
            show()
            return this
        }
    }

    override fun hide(message: Snackbar) {
        message.dismiss()
    }
}
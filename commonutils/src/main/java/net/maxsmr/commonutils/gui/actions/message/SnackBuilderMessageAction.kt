package net.maxsmr.commonutils.gui.actions.message

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.commonutils.text.EMPTY_STRING

data class SnackBuilderMessageAction(
        private val builder: Builder
): BaseMessageAction<Snackbar, View>() {

    override fun show(actor: View): Snackbar {
        return builder.build(actor).apply {
            show()
        }
    }

    override fun hide(message: Snackbar) {
        message.dismiss()
    }

    class Builder(
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
    ) {

        fun build(view: View): Snackbar {
            val message = if (messageResId != 0) {
                view.context.getString(messageResId)
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
                return this
            }
        }
    }
}
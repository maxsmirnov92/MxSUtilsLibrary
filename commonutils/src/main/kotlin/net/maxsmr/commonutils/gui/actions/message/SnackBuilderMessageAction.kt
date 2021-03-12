package net.maxsmr.commonutils.gui.actions.message

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage

data class SnackBuilderMessageAction(
        private val builder: Builder
) : BaseMessageAction<Snackbar, View>() {

    override fun show(actor: View): Snackbar {
        return builder.build(actor).apply {
            show()
        }
    }

    override fun hide(message: Snackbar) {
        message.dismiss()
    }

    data class Builder(
            val message: TextMessage? = null,
            @ColorRes
            val backgroundColorResId: Int = 0,
            @ColorInt
            val backgroundColor: Int? = null,
            val duration: Int = Snackbar.LENGTH_SHORT,
            val action: TextMessage? = null,
            @ColorRes
            val actionColorResId: Int = 0,
            val actionColor: Int? = null,
            val actionListener: ((view: View) -> Unit)? = null,
    ) {

        fun build(view: View): Snackbar {
            var duration = duration
            if (duration != Snackbar.LENGTH_SHORT
                    && duration != Snackbar.LENGTH_LONG
                    && duration != Snackbar.LENGTH_INDEFINITE) {
                duration = Snackbar.LENGTH_SHORT
            }
            val message = message?.get(view.context)
                    ?: throw IllegalStateException("message not specified")
            Snackbar.make(view, message, duration).apply {
                val backgroundColor: Int? = if (backgroundColorResId != 0) {
                    ContextCompat.getColor(view.context, backgroundColorResId)
                } else {
                    backgroundColor
                }
                if (backgroundColor != null) {
                    view.setBackgroundColor(backgroundColor)
                }
                action?.get(view.context)?.let {
                    setAction(it) { view -> actionListener?.invoke(view) }
                }
                val actionButtonColor = if (actionColorResId != 0) {
                    ContextCompat.getColor(view.context, actionColorResId)
                } else {
                    actionColor
                }
                actionButtonColor?.let {
                    setActionTextColor(actionButtonColor)
                }
                return this
            }
        }
    }
}
package net.maxsmr.commonutils.gui.actions.message

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage

/**
 * Действие для показа тоста
 */
data class ToastBuilderMessageAction(
        private val builder: Builder
) : BaseMessageAction<Toast, Context>() {

    @SuppressLint("ShowToast")
    override fun show(actor: Context): Toast {
        return builder.build(actor).apply {
            show()
        }
    }

    override fun hide(message: Toast) {
        message.cancel()
    }

    data class Builder(
            val message: TextMessage? = null,
            val gravity: Int? = null,
            val xOffset: Int = 0,
            val yOffset: Int = 0,
            val horizontalMargin: Float? = null,
            val verticalMargin: Float? = null,
            val duration: Int = Toast.LENGTH_SHORT,
            val customView: View? = null
    ) {

        @SuppressLint("ShowToast")
        fun build(context: Context): Toast {
            val toast: Toast
            var duration = duration
            if (duration != Toast.LENGTH_SHORT && duration != Toast.LENGTH_LONG) {
                duration = Toast.LENGTH_SHORT
            }
            if (customView == null) {
                val message = message?.get(context) ?: throw IllegalStateException("message not specified")
                toast = Toast.makeText(context, message, duration)
            } else {
                toast = Toast(context)
                toast.view = customView
                toast.duration = duration
            }
            gravity?.let {
                toast.setGravity(gravity, xOffset, yOffset)
            }
            if (horizontalMargin != null && verticalMargin != null) {
                toast.setMargin(horizontalMargin, verticalMargin)
            }
            return toast
        }
    }
}
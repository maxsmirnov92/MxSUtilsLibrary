package net.maxsmr.commonutils.android.gui.actions.message

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.data.text.EMPTY_STRING

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

    class Builder(
            val message: CharSequence = EMPTY_STRING,
            val messageResId: Int = 0,
            val gravity: Int? = null,
            val xOffset: Int = 0,
            val yOffset: Int = 0,
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
                val message = if (messageResId != 0) {
                    context.getString(messageResId)
                } else {
                    message
                }
                toast = Toast.makeText(context, message, duration)
            } else {
                toast = Toast(context)
                toast.view = customView
                toast.duration = duration
            }
            gravity?.let {
                toast.setGravity(gravity, xOffset, yOffset)
            }
            return toast
        }
    }
}
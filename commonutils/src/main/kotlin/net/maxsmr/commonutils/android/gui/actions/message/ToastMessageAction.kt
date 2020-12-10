package net.maxsmr.commonutils.android.gui.actions.message

import android.content.Context
import android.widget.Toast

class ToastMessageAction(
        message: Toast
): AnyMessageAction<Toast, Context>(
        message,
        { _, toast ->
            toast.show()
        },
        {
            it.cancel()
        }
)
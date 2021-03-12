package net.maxsmr.commonutils.gui.actions.message

import android.content.Context
import android.widget.Toast

@Deprecated("Toast.makeText is not recommended on ViewModel", replaceWith = ReplaceWith("ToastBuilderMessageAction"))
class ToastMessageAction(
        message: Toast
) : AnyMessageAction<Toast, Context>(
        message,
        { _, toast ->
            toast.show()
        },
        {
            it.cancel()
        }
)
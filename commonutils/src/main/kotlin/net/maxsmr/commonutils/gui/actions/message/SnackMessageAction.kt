package net.maxsmr.commonutils.gui.actions.message

import android.view.View
import com.google.android.material.snackbar.Snackbar

@Deprecated("Snackbar.make is not recommended on ViewModel" , replaceWith = ReplaceWith("SnackBuilderMessageAction"))
class SnackMessageAction(
        message: Snackbar
) : AnyMessageAction<Snackbar, View>(
        message,
        { _, snack ->
            snack.show()
        },
        {
            it.dismiss()
        }
)
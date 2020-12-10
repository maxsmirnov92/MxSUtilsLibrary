package net.maxsmr.commonutils.android.gui.actions.message

import android.view.View
import com.google.android.material.snackbar.Snackbar

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
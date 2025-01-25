package net.maxsmr.commonutils.gui.message

import androidx.annotation.StringRes
import net.maxsmr.commonutils.states.ILoadState

fun ILoadState.ErrorData.errorMessage(): TextMessage? {
    return message as? TextMessage
        ?: error.message?.takeIf { it.isNotEmpty() }?.let {
            TextMessage(it)
        }
}

fun TextMessage?.formatMessage(
    @StringRes messageWithArgResId: Int,
    @StringRes messageResId: Int,
): TextMessage {
    return this?.let {
        TextMessage(messageWithArgResId, it)
    } ?: TextMessage(messageResId)
}
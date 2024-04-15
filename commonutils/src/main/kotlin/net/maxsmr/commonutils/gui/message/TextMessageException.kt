package net.maxsmr.commonutils.gui.message

/**
 * [RuntimeException] с обёрнутым textMessage
 */
class TextMessageException(
    val textMessage: TextMessage,
    message: String? = null,
    cause: Throwable? = null
): RuntimeException()
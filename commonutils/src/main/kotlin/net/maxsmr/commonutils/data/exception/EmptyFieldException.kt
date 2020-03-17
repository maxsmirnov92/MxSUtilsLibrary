package net.maxsmr.commonutils.data.exception

import net.maxsmr.commonutils.data.text.EMPTY_STRING

/**
 * Ошибка пустого поля
 */
class EmptyFieldException(message: String = EMPTY_STRING) : Exception(message)
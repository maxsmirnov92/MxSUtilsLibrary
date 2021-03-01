package net.maxsmr.commonutils.exception

import net.maxsmr.commonutils.text.EMPTY_STRING

/**
 * Ошибка пустого поля
 */
class EmptyFieldException(message: String = EMPTY_STRING) : Exception(message)
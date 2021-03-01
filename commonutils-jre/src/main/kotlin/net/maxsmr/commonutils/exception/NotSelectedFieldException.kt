package net.maxsmr.commonutils.exception

import net.maxsmr.commonutils.text.EMPTY_STRING

/**
 * Ошибка невыбранного значения
 */
class NotSelectedFieldException(message: String = EMPTY_STRING) : Exception(message)
package net.maxsmr.commonutils.data.exception

import net.maxsmr.commonutils.data.text.EMPTY_STRING

/**
 * Ошибка невыбранного значения
 */
class NotSelectedFieldException(message: String = EMPTY_STRING) : Exception(message)
package net.maxsmr.commonutils.data.exception

import net.maxsmr.commonutils.data.text.EMPTY_STRING

/**
 * Ошибка невалидных данных
 */
class NotValidFieldException(message: String = EMPTY_STRING, cause: Throwable? = null) : Exception(message, cause)

/**
 * @return если это [NotValidFieldException], то неизменный,
 * в противном случае - завернуть в cause
 */
fun Throwable?.toNotValidFieldException()
        : NotValidFieldException? = when {
    this is NotValidFieldException -> this
    this != null -> NotValidFieldException(cause = this)
    else -> null
}

/**
 * см. [toNotValidFieldException]
 */
fun <T> Pair<T?, Throwable?>.toPairWithNotValidFieldException()
        : Pair<T?, NotValidFieldException?> = Pair(first, second.toNotValidFieldException())
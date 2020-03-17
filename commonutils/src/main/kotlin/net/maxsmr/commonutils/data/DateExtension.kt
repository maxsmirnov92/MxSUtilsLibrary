package net.maxsmr.commonutils.data

import java.util.*

fun Date.isLess(second: Date?): Boolean {
    second ?: return false
    return this < second // "<"
}

fun Date.isGreater(second: Date?): Boolean {
    second ?: return false
    return this > second // ">"
}

fun Date.isLessOrEquals(second: Date?): Boolean {
    second ?: return false
    return this <= second // "<="
}

fun Date.isGreaterOrEquals(second: Date?): Boolean {
    second ?: return false
    return this >= second // ">="
}

fun Date.isEquals(second: Date?): Boolean {
    second ?: return false
    return this.compareTo(second) == 0 // "=="
}

/** @return true, если эта дата уже прошла (< текущей);
 * считаем, что всегда находимся в начале текущего дня
 */
fun Date.passed() = isLess(Date().toDayStart())

fun Date.toDayStart(): Date =
        Calendar.getInstance()
                .also {
                    it.time = this
                    it.set(Calendar.HOUR_OF_DAY, 0)
                    it.set(Calendar.MINUTE, 0)
                    it.set(Calendar.SECOND, 0)
                    it.set(Calendar.MILLISECOND, 0)
                }.time

fun Date.toDayEnd(): Date =
        Calendar.getInstance()
                .also {
                    it.time = this
                    it.set(Calendar.HOUR_OF_DAY, 23)
                    it.set(Calendar.MINUTE, 59)
                    it.set(Calendar.SECOND, 59)
                    it.set(Calendar.MILLISECOND, 999)
                }.time

fun Date.toMonthStart(): Date =
        Calendar.getInstance()
                .also {
                    it.time = this
                    it.set(Calendar.DAY_OF_MONTH, 1)
                }.time
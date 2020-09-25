package net.maxsmr.commonutils.data.sort

import java.util.*

abstract class BaseOptionalComparator <O : ISortOption, T>(
        private val sortOptions: Map<O, Boolean> = mutableMapOf()
) : Comparator<T> {


    override fun compare(lhs: T, rhs: T): Int {
        var result = 0
        for ((option, ascending) in sortOptions) {
            result = compare(lhs, rhs, option, ascending)
            if (result != 0) {
                break
            }
        }
        return result
    }

    protected abstract fun compare(lhs: T?, rhs: T?, option: O, ascending: Boolean): Int
}
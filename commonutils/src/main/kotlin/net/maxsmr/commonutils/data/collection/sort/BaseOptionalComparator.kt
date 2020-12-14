package net.maxsmr.commonutils.data.collection.sort

import net.maxsmr.commonutils.data.collection.toMapIndexed
import java.util.*

abstract class BaseOptionalComparator<O : ISortOption, T>(
        private val sortOptions: Map<O, Boolean?> = mutableMapOf()
) : Comparator<T> {

    constructor(options: Collection<O>) : this(
            options.toMapIndexed<O, Boolean?> { _, _ -> null }
    )

    final override fun compare(lhs: T?, rhs: T?): Int {
        var result = 0
        for ((option, ascending) in sortOptions) {
            if (lhs != null && rhs != null) {
                result = compare(lhs, rhs, option, ascending ?: true)
                if (result != 0) {
                    break
                }
            } else {
                result = if (lhs == null && rhs == null) {
                    0
                } else {
                    if (ascending != false) {
                        if (lhs != null) {
                            1
                        } else {
                            -1
                        }
                    } else {
                        if (rhs != null) {
                            1
                        } else {
                            -1
                        }
                    }
                }
            }
        }
        return result
    }

    protected abstract fun compare(lhs: T, rhs: T, option: O, ascending: Boolean): Int
}
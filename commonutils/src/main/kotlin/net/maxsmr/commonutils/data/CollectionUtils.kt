package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.entity.Valuable

fun <N : Number> findMinValuable(collection: Collection<Valuable<N?>?>): Pair<Int, N>? = findValuable(collection, true)

fun <N : Number> findMaxValuable(collection: Collection<Valuable<N?>?>): Pair<Int, N>? = findValuable(collection, false)

fun <N : Number> findMin(collection: Collection<N?>): Pair<Int, N>? = find(collection, true)

fun <N : Number> findMax(collection: Collection<N?>): Pair<Int, N>? = find(collection, false)

private fun <N : Number> findValuable(collection: Collection<Valuable<N?>?>, isMin: Boolean): Pair<Int, N>? {
    return find(collection.map { it?.value }, isMin)
}

private fun <N : Number> find(collection: Collection<N?>, isMin: Boolean): Pair<Int, N>? {
    var result: Pair<Int, N>? = null
    collection.forEachIndexed { index, value ->
        if (value != null) {
            result.let {
                if (it == null ||
                        if (isMin) {
                            compareNumbers(value, it.second, true) < 0
                        } else {
                            compareNumbers(value, it.second, true) > 0
                        }) {
                    result = Pair(index, value)
                }
            }
        }
    }
    return result
}
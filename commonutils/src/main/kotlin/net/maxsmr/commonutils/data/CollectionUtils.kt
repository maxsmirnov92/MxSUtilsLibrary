package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.text.isNotZeroOrNull

@JvmOverloads
fun <V> toMutableListExclude(
        collection: Collection<V>?,
        exclusionCollection: Collection<V>,
        containsPredicate: (V, V) -> Boolean = { one, another -> one == another }
): List<V> = fromCollectionExclude(mutableListOf(), collection, exclusionCollection, containsPredicate)

@JvmOverloads
fun <V> toSortedSetExclude(
        collection: Collection<V>?,
        exclusionCollection: Collection<V>,
        containsPredicate: (V, V) -> Boolean = { one, another -> one == another }
): Set<V> = fromCollectionExclude(sortedSetOf(), collection, exclusionCollection, containsPredicate)

@JvmOverloads
fun <V, C : MutableCollection<V>> fromCollectionExclude(
        result: C,
        collection: Collection<V>?,
        exclusionCollection: Collection<V>,
        containsPredicate: (V, V) -> Boolean = { one, another -> one == another }
): C {
    collection?.let {
        collection.forEach { current ->
            if (!Predicate.Methods.contains(exclusionCollection) {
                        containsPredicate(current, it)
                    }) {
                result.add(current)
            }
        }
    }
    return result
}

fun avg(numbers: Collection<Number?>?): Double {
    var result = 0.0
    numbers?.let {
        val count = numbers.size
        var sum = 0.0
        for (n in numbers) {
            if (n != null) {
                sum += n.toDouble()
            }
        }
        result = sum / count
    }
    return result
}

fun sum(numbers: Collection<Number?>?): Double {
    var result = 0.0
    if (numbers != null) {
        for (n in numbers) {
            if (n != null) {
                result += n.toDouble()
            }
        }
    }
    return result
}

fun findMin(collection: Collection<Number?>?): Pair<Int, Number>? = find(collection, true)

fun findMax(collection: Collection<Number?>?): Pair<Int, Number>? = find(collection, false)

private fun find(collection: Collection<Number?>?, isMin: Boolean): Pair<Int, Number>? {
    var result: Pair<Int, Number>? = null
    collection?.forEachIndexed { index, value ->
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
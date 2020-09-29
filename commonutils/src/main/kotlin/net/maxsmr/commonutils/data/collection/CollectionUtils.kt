package net.maxsmr.commonutils.data.collection

import net.maxsmr.commonutils.data.Pair
import net.maxsmr.commonutils.data.Predicate
import net.maxsmr.commonutils.data.compareNumbers

@JvmOverloads
fun <V> Collection<V>?.toMutableListExclude(
        exclusionCollection: Collection<V>,
        containsPredicate: (V, V) -> Boolean = { one, another -> one == another }
): List<V> = fromCollectionExclude(mutableListOf(), exclusionCollection, containsPredicate)

@JvmOverloads
fun <V> Collection<V>?.toSortedSetExclude(
        exclusionCollection: Collection<V>,
        containsPredicate: (V, V) -> Boolean = { one, another -> one == another }
): Set<V> = fromCollectionExclude(sortedSetOf(), exclusionCollection, containsPredicate)

@JvmOverloads
fun <V, C : MutableCollection<V>> Collection<V>?.fromCollectionExclude(
        result: C,
        exclusionCollection: Collection<V>,
        containsPredicate: (V, V) -> Boolean = { one, another -> one == another }
): C {
    this?.let {
        it.forEach { current ->
            if (!Predicate.Methods.contains(exclusionCollection) {
                        containsPredicate(current, it)
                    }) {
                result.add(current)
            }
        }
    }
    return result
}

fun <K, V> Collection<K>?.toMapIndexed(valueForKey: (Int, K) -> V): Map<K, V> {
    val result = mutableMapOf<K, V>()
    this?.let {
        it.forEachIndexed { index, key ->
            result[key] = valueForKey(index, key)
        }
    }
    return result
}

fun <T> Collection<T>?.limit(limit: Int): Collection<T> {
    if (limit <= 0 || this == null || this.isEmpty() || limit > this.size) return this ?: emptyList()
    return this.toList().subList(0, limit - 1)
}

fun Collection<Number?>?.avg(): Double {
    var result = 0.0
    this?.let {
        val count = this.size
        var sum = 0.0
        for (n in this) {
            if (n != null) {
                sum += n.toDouble()
            }
        }
        result = sum / count
    }
    return result
}

fun Collection<Number?>?.sum(): Double {
    var result = 0.0
    if (this != null) {
        for (n in this) {
            if (n != null) {
                result += n.toDouble()
            }
        }
    }
    return result
}

fun Collection<Number?>?.min(): Pair<Int, Number>? = find(true)

fun Collection<Number?>?.max(): Pair<Int, Number>? = find(false)

private fun Collection<Number?>?.find(isMin: Boolean): Pair<Int, Number>? {
    var result: Pair<Int, Number>? = null
    this?.forEachIndexed { index, value ->
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
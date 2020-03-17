package net.maxsmr.commonutils.data

import net.maxsmr.commonutils.data.text.isNotZeroOrNull

fun <K, V : Number> putIfNotNullOrZero(map: MutableMap<K, V>, key: K, value: V?) {
    if (value != null && isNotZeroOrNull(value.toString())) {
        map[key] = value
    }
}

fun avg(numbers: Collection<Number?>?): Double {
    var result = 0.0
    if (numbers != null) {
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
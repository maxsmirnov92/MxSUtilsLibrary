package net.maxsmr.commonutils.data.number

import java.util.*

/**
 * Returns a pseudo-random number between min and max, inclusive.
 * The difference between min and max can be at most
 * `Integer.MAX_VALUE - 1`.
 *
 * @param min Minimum value
 * @param max Maximum value.  Must be greater than min.
 * @return Integer between min and max, inclusive.
 * @see java.util.Random.nextInt
 */
fun randInt(min: Int, max: Int): Int { // NOTE: This will (intentionally) not run as written so that folks
// copy-pasting have to think about how to initialize their
// Random instance.  Initialization of the Random instance is outside
// the main scope of the question, but some decent options are to have
// a field that is initialized once and then re-used as needed or to
// use ThreadLocalRandom (if using at least Java 1.7).
    val rand = Random()
    // nextInt is normally exclusive of the top value,
// so add 1 to make it inclusive
    return rand.nextInt(max - min + 1) + min
}

fun randLong(min: Long, max: Long): Long {
    return Random().nextLong() % (max - min) + min
}

fun generateNumber(excludeNumbers: Collection<Number?>?, minValue: Int, maxValue: Int): Int? {
    require(minValue <= maxValue) { "minValue ($minValue) > maxValue ($maxValue)" }
    val rangeSize = maxValue - minValue
    val checkedCodes: MutableSet<Int> = LinkedHashSet()
    var found = true
    var newCode = minValue
    if (excludeNumbers != null && !excludeNumbers.isEmpty()) {
        found = false
        var iterations = 0
        val excludeNumbersCopy = excludeNumbers.toList()
        var i = 0
        while (i < excludeNumbersCopy.size && iterations <= rangeSize * 2) {
            val code = excludeNumbersCopy[i]
            if (code != null && code.toInt() == newCode) {
                newCode = randInt(minValue, maxValue)
                if (!checkedCodes.contains(newCode)) {
                    checkedCodes.add(newCode)
                    i = 0
                    iterations++
                }
            } else {
                if (i == excludeNumbers.size - 1) {
                    found = true
                }
            }
            i++
        }
    }
    return if (found) newCode else null
}
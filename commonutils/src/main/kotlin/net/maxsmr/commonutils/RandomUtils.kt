package net.maxsmr.commonutils

import java.util.*
import java.util.concurrent.ThreadLocalRandom

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
    require(max > min) { "max ($max) should be more than min ($min)" }
    return if (isAtLeastLollipop()) {
        ThreadLocalRandom.current().nextInt(min, max + 1);
    } else {
        // In particular, do NOT do 'Random rand = new Random()' here or you
        // will get not very good / not very random results.
        val rand = Random()
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        rand.nextInt(max - min + 1) + min
    }
}

@JvmOverloads
fun randIntUnique(
        min: Int,
        max: Int,
        existingInts: Collection<Int>,
        tryCountLimit: Int = 0
): Int {
    var result: Int
    var tryCount = 0
    do {
        result = randInt(min, max)
        tryCount++
    } while (existingInts.contains(result)
            && (tryCountLimit <= 0 || tryCount < tryCountLimit))
    return result
}
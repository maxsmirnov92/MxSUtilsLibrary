package net.maxsmr.commonutils

import android.os.Bundle

fun bundleEquals(one: Bundle?, two: Bundle?): Boolean {
    if (one == null && two == null) return false
    if (one?.size() ?: 0 != two?.size() ?: 0) return false
    if (one != null) {
        if (two != null) {
            for (key in one.keySet()) {
                val valueOne = one[key]
                val valueTwo = two[key]
                if (valueOne is Bundle && valueTwo is Bundle &&
                        !bundleEquals(valueOne, valueTwo)) {
                    return false
                } else if (valueOne == null) {
                    if (valueTwo != null || !two.containsKey(key)) return false
                } else if (valueOne != valueTwo) return false
            }
            return true
        }
    }
    return false
}
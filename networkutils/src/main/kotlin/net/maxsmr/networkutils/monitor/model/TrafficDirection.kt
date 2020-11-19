package net.maxsmr.networkutils.monitor.model

enum class TrafficDirection(val value: Int) {
    NONE(-1), RECEIVE(0), TRANSMIT(1), BOTH(2);

    companion object {

        fun fromValue(value: Int): TrafficDirection? = try {
            fromValueOrThrow(value)
        } catch (e: IllegalArgumentException) {
            null
        }

        @Throws(IllegalArgumentException::class)
        fun fromValueOrThrow(value: Int): TrafficDirection = when (value) {
                -1 -> NONE
                0 -> RECEIVE
                1 -> TRANSMIT
                2 -> BOTH
                else -> throw IllegalArgumentException("No $value for enum type " + TrafficDirection::class.java.name)
            }
    }
}
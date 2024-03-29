package net.maxsmr.commonutils.conversion

import net.maxsmr.commonutils.collection.toSortedSetExclude
import net.maxsmr.commonutils.number.*
import java.math.BigDecimal

enum class SizeUnit {
    BYTES {
        override fun toBytes(s: Double): Long {
            return s.toLong()
        }

        override fun toKBytes(s: Double): Double {
            return s / C1
        }

        override fun toMBytes(s: Double): Double {
            return s / C2
        }

        override fun toGBytes(s: Double): Double {
            return s / C3
        }

        override fun toBits(s: Double): Long {
            return toBitsFromBytes(s.toLong())
        }

        override fun toKBits(s: Double): Long {
            return toBitsFromBytes(toKBytes(s).toLong())
        }

        override fun toMBits(s: Double): Long {
            return toBitsFromBytes(toMBytes(s).toLong())
        }

        override fun toGBits(s: Double): Long {
            return toBitsFromBytes(toGBytes(s).toLong())
        }
    },
    KBYTES {
        override fun toBytes(s: Double): Long {
            return (s * C1).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return s
        }

        override fun toMBytes(s: Double): Double {
            return s / C1
        }

        override fun toGBytes(s: Double): Double {
            return s / C2
        }

        override fun toBits(s: Double): Long {
            return toBitsFromBytes(s.toLong())
        }

        override fun toKBits(s: Double): Long {
            return toBitsFromBytes(toKBytes(s).toLong())
        }

        override fun toMBits(s: Double): Long {
            return toBitsFromBytes(toMBytes(s).toLong())
        }

        override fun toGBits(s: Double): Long {
            return toBitsFromBytes(toGBytes(s).toLong())
        }
    },
    MBYTES {
        override fun toBytes(s: Double): Long {
            return (s * C2).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return s * C1
        }

        override fun toMBytes(s: Double): Double {
            return s
        }

        override fun toGBytes(s: Double): Double {
            return s / C1
        }

        override fun toBits(s: Double): Long {
            return toBitsFromBytes(s.toLong())
        }

        override fun toKBits(s: Double): Long {
            return toBitsFromBytes(toKBytes(s).toLong())
        }

        override fun toMBits(s: Double): Long {
            return toBitsFromBytes(toMBytes(s).toLong())
        }

        override fun toGBits(s: Double): Long {
            return toBitsFromBytes(toGBytes(s).toLong())
        }
    },
    GBYTES {
        override fun toBytes(s: Double): Long {
            return (s * C3).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return s * C2
        }

        override fun toMBytes(s: Double): Double {
            return s * C1
        }

        override fun toGBytes(s: Double): Double {
            return s
        }

        override fun toBits(s: Double): Long {
            return toBitsFromBytes(s.toLong())
        }

        override fun toKBits(s: Double): Long {
            return toBitsFromBytes(toKBytes(s).toLong())
        }

        override fun toMBits(s: Double): Long {
            return toBitsFromBytes(toMBytes(s).toLong())
        }

        override fun toGBits(s: Double): Long {
            return toBitsFromBytes(toGBytes(s).toLong())
        }
    },
    BITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s.toLong())
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s)).toDouble()
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s)).toDouble()
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s)).toDouble()
        }

        override fun toBits(s: Double): Long {
            return s.toLong()
        }

        override fun toKBits(s: Double): Long {
            return (s / C1).toLong()
        }

        override fun toMBits(s: Double): Long {
            return (s / C2).toLong()
        }

        override fun toGBits(s: Double): Long {
            return (s / C3).toLong()
        }
    },
    KBITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s.toLong())
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s)).toDouble()
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s)).toDouble()
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s)).toDouble()
        }

        override fun toBits(s: Double): Long {
            return (s * C1).toLong()
        }

        override fun toKBits(s: Double): Long {
            return s.toLong()
        }

        override fun toMBits(s: Double): Long {
            return (s / C2).toLong()
        }

        override fun toGBits(s: Double): Long {
            return (s / C3).toLong()
        }
    },
    MBITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s.toLong())
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s)).toDouble()
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s)).toDouble()
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s)).toDouble()
        }

        override fun toBits(s: Double): Long {
            return (s * C2).toLong()
        }

        override fun toKBits(s: Double): Long {
            return (s * C1).toLong()
        }

        override fun toMBits(s: Double): Long {
            return s.toLong()
        }

        override fun toGBits(s: Double): Long {
            return (s / C1).toLong()
        }
    },
    GBITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s.toLong())
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s)).toDouble()
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s)).toDouble()
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s)).toDouble()
        }

        override fun toBits(s: Double): Long {
            return (s * C3).toLong()
        }

        override fun toKBits(s: Double): Long {
            return (s * C2).toLong()
        }

        override fun toMBits(s: Double): Long {
            return (s * C1).toLong()
        }

        override fun toGBits(s: Double): Long {
            return s.toLong()
        }
    };

    abstract fun toBytes(s: Double): Long
    abstract fun toKBytes(s: Double): Double
    abstract fun toMBytes(s: Double): Double
    abstract fun toGBytes(s: Double): Double
    abstract fun toBits(s: Double): Long
    abstract fun toKBits(s: Double): Long
    abstract fun toMBits(s: Double): Long
    abstract fun toGBits(s: Double): Long

    val isBits: Boolean
        get() = this === BITS || this === KBITS || this === MBITS || this === GBITS

    val isBytes: Boolean
        get() = this === BYTES || this === KBYTES || this === MBYTES || this === GBYTES

    companion object {

        const val C0: Long = 8
        const val C1 = 1024L
        const val C2 = C1 * 1024L
        const val C3 = C2 * 1024L

        fun toBitsFromBytes(s: Long): Long {
            return s * C0
        }

        fun toBytesFromBits(s: Long): Long {
            return (s.toDouble() / C0).toLong()
        }

        fun convert(what: Long, from: SizeUnit, to: SizeUnit): Number {
            return when (to) {
                BITS -> from.toBits(what.toDouble())
                BYTES -> from.toBytes(what.toDouble())
                KBITS -> from.toKBits(what.toDouble())
                KBYTES -> from.toKBytes(what.toDouble())
                MBITS -> from.toMBits(what.toDouble())
                MBYTES -> from.toMBytes(what.toDouble())
                GBITS -> from.toGBits(what.toDouble())
                GBYTES -> from.toGBytes(what.toDouble())
            }
        }
    }
}

// TODO timeUnitsToInclude
/**
 * @param unit for [size]
 * @param sizeUnitsToExclude list of units to avoid in result string
 * @param precision null - not allow fraction, 0 - unlimited
 */
@JvmOverloads
fun sizeToMap(
        size: Double,
        sizeUnit: SizeUnit,
        sizeUnitsToExclude: Set<SizeUnit> = setOf(),
        precision: Int? = null
): Map<SizeUnit, Number> {
    require(size >= 0) { "Incorrect size: $size" }
    require(sizeUnit.isBytes) { "sizeUnit must be bytes only" }

    val s = sizeUnit.toBytes(size)

    val result = sortedMapOf<SizeUnit, Number>()

    if (s >= SizeUnit.C3 && !sizeUnitsToExclude.contains(SizeUnit.GBYTES)) {
        val gBytes = SizeUnit.BYTES.toGBytes(s.toDouble())
        val gBytesLong = gBytes.toLong().toDouble()
        if (gBytes.isNotZero()) {
            result[SizeUnit.GBYTES] = gBytes
        }
        result.putAll(sizeToMapStep(SizeUnit.GBYTES.toBytes(gBytesLong), s, sizeUnitsToExclude, precision))
    } else if ((sizeUnitsToExclude.contains(SizeUnit.GBYTES) || s >= SizeUnit.C2 && s < SizeUnit.C3)
            && !sizeUnitsToExclude.contains(SizeUnit.MBYTES)) {
        val mBytes = SizeUnit.BYTES.toMBytes(s.toDouble())
        val mBytesLong = mBytes.toLong().toDouble()
        if (mBytes.isNotZero()) {
            result[SizeUnit.MBYTES] = mBytes
        }
        result.putAll(sizeToMapStep(SizeUnit.MBYTES.toBytes(mBytesLong), s, sizeUnitsToExclude.toSortedSetExclude(setOf(SizeUnit.GBYTES)), precision))
    } else if ((sizeUnitsToExclude.contains(SizeUnit.MBYTES) || s >= SizeUnit.C1 && s < SizeUnit.C2)
            && !sizeUnitsToExclude.contains(SizeUnit.KBYTES)) {
        val kBytes = SizeUnit.BYTES.toKBytes(s.toDouble())
        val kBytesLong = kBytes.toLong().toDouble()
        if (kBytes.isNotZero()) {
            result[SizeUnit.KBYTES] = kBytes
        }
        result.putAll(sizeToMapStep(SizeUnit.KBYTES.toBytes(kBytesLong), s, sizeUnitsToExclude.toSortedSetExclude(setOf(SizeUnit.MBYTES)), precision))
    } else if (s < SizeUnit.C1 && !sizeUnitsToExclude.contains(SizeUnit.BYTES)) {
        if (s != 0L) {
            result[SizeUnit.BYTES] = s
        }
    }

    val iterator = result.toSortedMap(reverseOrder()).iterator()
    result.clear()
    while (iterator.hasNext()) {
        val current = iterator.next()
        val fraction = current.value.toBigDecimal().fraction()
        result[current.key] = if (precision == null ||
                fraction.isZero() ||
                fraction.isGreater(BigDecimal.ZERO) && iterator.hasNext()) {
            current.value.toLong()
        } else {
            if (precision == 0) {
                current.value
            } else {
                current.value.toDouble().round(precision)
            }
        }
    }

    return result.toSortedMap(reverseOrder())
}

private fun sizeToMapStep(
        currentBytes: Long,
        sourceBytes: Long,
        sizeUnitsToExclude: Set<SizeUnit>,
        precision: Int?
): Map<SizeUnit, Number> {
    if (currentBytes > 0) {
        val restBytes = sourceBytes - currentBytes
        if (restBytes > 0) {
            return sizeToMap(restBytes.toDouble(), SizeUnit.BYTES, sizeUnitsToExclude, precision)
        }
    }
    return emptyMap()
}
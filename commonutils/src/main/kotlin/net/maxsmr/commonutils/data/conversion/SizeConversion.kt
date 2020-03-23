package net.maxsmr.commonutils.data.conversion

import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.data.number.fraction
import net.maxsmr.commonutils.data.number.isGreater
import net.maxsmr.commonutils.data.number.isZero
import net.maxsmr.commonutils.data.number.round
import net.maxsmr.commonutils.data.putIfNotNullOrZero
import net.maxsmr.commonutils.data.text.join
import net.maxsmr.commonutils.data.toSortedSetExclude
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
                else -> 0
            }
        }
    }
}

/**
 * @param sizeUnit unit for [size]
 * @param sizeUnitsToExclude list of units to avoid in result string
 * @param precision null - not allow fraction, 0 - unlimited
 */
fun sizeToString(
        size: Double,
        sizeUnit: SizeUnit,
        sizeUnitsToExclude: Set<SizeUnit> = setOf(),
        precision: Int? = null,
        stringsProvider: (Int, Number?) -> String
): String {
    val result = mutableListOf<String>()
    val map = sizeToMap(size, sizeUnit, sizeUnitsToExclude, precision)
    val timeFormat = stringsProvider(R.string.size_format, null)
    map.forEach {
        when (it.key) {
            SizeUnit.BYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.size_suffix_bytes, it.value)))
            }
            SizeUnit.KBYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.size_suffix_kbytes, it.value)))
            }
            SizeUnit.MBYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.size_suffix_mbytes, it.value)))
            }
            SizeUnit.GBYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.plurals.size_suffix_gbytes, it.value)))
            }
            else -> {
                // do noting
            }
        }
    }
    return join(", ", result)
}

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

    val notAllowFractional = precision == null
    val s = sizeUnit.toBytes(size)

    val result = sortedMapOf<SizeUnit, Number>()

    if (s >= SizeUnit.C3 && !sizeUnitsToExclude.contains(SizeUnit.GBYTES)) {
        val gBytes = SizeUnit.BYTES.toGBytes(s.toDouble())
        val gBytesLong = gBytes.toLong().toDouble()
        putIfNotNullOrZero(result, SizeUnit.GBYTES, if (notAllowFractional || !sizeUnitsToExclude.contains(SizeUnit.MBYTES)) gBytesLong else gBytes)
        result.putAll(sizeToMapStep(SizeUnit.GBYTES.toBytes(gBytesLong), s, sizeUnitsToExclude, precision))
    } else if ((sizeUnitsToExclude.contains(SizeUnit.GBYTES) || s >= SizeUnit.C2 && s < SizeUnit.C3)
            && !sizeUnitsToExclude.contains(SizeUnit.MBYTES)) {
        val mBytes = SizeUnit.BYTES.toMBytes(s.toDouble())
        val mBytesLong = mBytes.toLong().toDouble()
        putIfNotNullOrZero(result, SizeUnit.MBYTES, if (notAllowFractional ||!sizeUnitsToExclude.contains(SizeUnit.KBYTES)) mBytesLong else mBytes)
        result.putAll(sizeToMapStep(SizeUnit.MBYTES.toBytes(mBytesLong), s, toSortedSetExclude(sizeUnitsToExclude, setOf(SizeUnit.GBYTES)), precision))
    } else if ((sizeUnitsToExclude.contains(SizeUnit.MBYTES) || s >= SizeUnit.C1 && s < SizeUnit.C2)
            && !sizeUnitsToExclude.contains(SizeUnit.KBYTES)) {
        val kBytes = SizeUnit.BYTES.toKBytes(s.toDouble())
        val kBytesLong = kBytes.toLong().toDouble()
        // дополнительная проверка на наличие в исключениях предыдущего Unit (т.е. Bytes) - если есть там, то оставить дробное значение
        putIfNotNullOrZero(result, SizeUnit.KBYTES, if (notAllowFractional ||!sizeUnitsToExclude.contains(SizeUnit.BYTES)) kBytesLong else kBytes)
        result.putAll(sizeToMapStep(SizeUnit.KBYTES.toBytes(kBytesLong), s, toSortedSetExclude(sizeUnitsToExclude, setOf(SizeUnit.MBYTES)), precision))
    } else if (s < SizeUnit.C1 && !sizeUnitsToExclude.contains(SizeUnit.BYTES)) {
        putIfNotNullOrZero(result, SizeUnit.BYTES, s.toDouble())
    }

    val iterator = result.toSortedMap(reverseOrder()).iterator()
    result.clear()
    while (iterator.hasNext()) {
        val current = iterator.next()
        val fraction = current.value.fraction()
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
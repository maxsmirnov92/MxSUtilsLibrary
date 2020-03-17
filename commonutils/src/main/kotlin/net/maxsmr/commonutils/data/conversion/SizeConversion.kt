package net.maxsmr.commonutils.data.conversion

import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.data.text.join
import net.maxsmr.commonutils.data.putIfNotNullOrZero

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
            return toBitsFromBytes(s).toLong()
        }

        override fun toKBits(s: Double): Double {
            return toBitsFromBytes(toKBytes(s))
        }

        override fun toMBits(s: Double): Double {
            return toBitsFromBytes(toMBytes(s))
        }

        override fun toGBits(s: Double): Double {
            return toBitsFromBytes(toGBytes(s))
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
            return toBitsFromBytes(s).toLong()
        }

        override fun toKBits(s: Double): Double {
            return toBitsFromBytes(toKBytes(s))
        }

        override fun toMBits(s: Double): Double {
            return toBitsFromBytes(toMBytes(s))
        }

        override fun toGBits(s: Double): Double {
            return toBitsFromBytes(toGBytes(s))
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
            return toBitsFromBytes(s).toLong()
        }

        override fun toKBits(s: Double): Double {
            return toBitsFromBytes(toKBytes(s))
        }

        override fun toMBits(s: Double): Double {
            return toBitsFromBytes(toMBytes(s))
        }

        override fun toGBits(s: Double): Double {
            return toBitsFromBytes(toGBytes(s))
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
            return toBitsFromBytes(s).toLong()
        }

        override fun toKBits(s: Double): Double {
            return toBitsFromBytes(toKBytes(s))
        }

        override fun toMBits(s: Double): Double {
            return toBitsFromBytes(toMBytes(s))
        }

        override fun toGBits(s: Double): Double {
            return toBitsFromBytes(toGBytes(s))
        }
    },
    BITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s))
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s))
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s))
        }

        override fun toBits(s: Double): Long {
            return s.toLong()
        }

        override fun toKBits(s: Double): Double {
            return s / C1
        }

        override fun toMBits(s: Double): Double {
            return s / C2
        }

        override fun toGBits(s: Double): Double {
            return s / C3
        }
    },
    KBITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s))
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s))
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s))
        }

        override fun toBits(s: Double): Long {
            return (s * C1).toLong()
        }

        override fun toKBits(s: Double): Double {
            return s
        }

        override fun toMBits(s: Double): Double {
            return s / C2
        }

        override fun toGBits(s: Double): Double {
            return s / C3
        }
    },
    MBITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s))
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s))
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s))
        }

        override fun toBits(s: Double): Long {
            return (s * C2).toLong()
        }

        override fun toKBits(s: Double): Double {
            return s * C1
        }

        override fun toMBits(s: Double): Double {
            return s
        }

        override fun toGBits(s: Double): Double {
            return s / C1
        }
    },
    GBITS {
        override fun toBytes(s: Double): Long {
            return toBytesFromBits(s).toLong()
        }

        override fun toKBytes(s: Double): Double {
            return toBytesFromBits(toKBits(s))
        }

        override fun toMBytes(s: Double): Double {
            return toBytesFromBits(toMBits(s))
        }

        override fun toGBytes(s: Double): Double {
            return toBytesFromBits(toGBits(s))
        }

        override fun toBits(s: Double): Long {
            return (s * C3).toLong()
        }

        override fun toKBits(s: Double): Double {
            return s * C2
        }

        override fun toMBits(s: Double): Double {
            return s * C1
        }

        override fun toGBits(s: Double): Double {
            return s
        }
    };

    abstract fun toBytes(s: Double): Long
    abstract fun toKBytes(s: Double): Double
    abstract fun toMBytes(s: Double): Double
    abstract fun toGBytes(s: Double): Double
    abstract fun toBits(s: Double): Long
    abstract fun toKBits(s: Double): Double
    abstract fun toMBits(s: Double): Double
    abstract fun toGBits(s: Double): Double

    val isBits: Boolean
        get() = this === BITS || this === KBITS || this === MBITS || this === GBITS

    val isBytes: Boolean
        get() = this === BYTES || this === KBYTES || this === MBYTES || this === GBYTES

    companion object {

        const val C0: Long = 8
        const val C1 = 1024L
        const val C2 = C1 * 1024L
        const val C3 = C2 * 1024L

        fun toBitsFromBytes(s: Double): Double {
            return s * C0
        }

        fun toBytesFromBits(s: Double): Double {
            return s / C0
        }

        fun convert(what: Long, from: SizeUnit, to: SizeUnit): Double {
            return when (to) {
                BITS -> from.toBits(what.toDouble()).toDouble()
                BYTES -> from.toBytes(what.toDouble()).toDouble()
                KBITS -> from.toKBits(what.toDouble())
                KBYTES -> from.toKBytes(what.toDouble())
                MBITS -> from.toMBits(what.toDouble())
                MBYTES -> from.toMBytes(what.toDouble())
                GBITS -> from.toGBits(what.toDouble())
                GBYTES -> from.toGBytes(what.toDouble())
                else -> 0.0
            }
        }
    }
}

/**
 * @param sizeUnit           unit for s
 * @param sizeUnitsToExclude list of units to avoid in result string
 */
fun sizeToString(
        size: Double,
        sizeUnit: SizeUnit,
        sizeUnitsToExclude: Set<SizeUnit> = setOf(),
        stringsProvider: (Int) -> String
): String {
    val result = mutableListOf<String>()
    val map = sizeToMap(size, sizeUnit, sizeUnitsToExclude)
    val timeFormat = stringsProvider(R.string.size_format)
    map.forEach {
        when (it.key) {
            SizeUnit.BYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.size_suffix_bytes)))
            }
            SizeUnit.KBYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.size_suffix_kbytes)))
            }
            SizeUnit.MBYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.size_suffix_mbytes)))
            }
            SizeUnit.GBYTES -> {
                result.add(String.format(timeFormat, it.value, stringsProvider(R.string.size_suffix_gbytes)))
            }
            else -> {
                // do noting
            }
        }
    }
    return join(",", result)
}

/**
 * @param size                  size
 * @param sizeUnit           unit for s
 * @param sizeUnitsToExclude list of units to avoid in result string
 */
fun sizeToMap(
        size: Double,
        sizeUnit: SizeUnit,
        sizeUnitsToExclude: Set<SizeUnit> = setOf()
): Map<SizeUnit, Double> {
    require(size >= 0) { "Incorrect size: $size" }
    require(sizeUnit.isBytes) { "sizeUnit must be bytes only" }

    val s = sizeUnit.toBytes(size)

    val result = sortedMapOf<SizeUnit, Double>()

    if (s < SizeUnit.C1 && !sizeUnitsToExclude.contains(SizeUnit.BYTES)) {
        putIfNotNullOrZero(result, SizeUnit.BYTES, s.toDouble())
    } else if ((sizeUnitsToExclude.contains(SizeUnit.BYTES) || s >= SizeUnit.C1 && s < SizeUnit.C2)
            && !sizeUnitsToExclude.contains(SizeUnit.KBYTES)) {
        val kBytes = SizeUnit.BYTES.toKBytes(s.toDouble())
        // дополнительная проверка на наличие в исключениях предыдущего Unit (т.е. Bytes) - если есть там, то оставить дробное значение
        putIfNotNullOrZero(result, SizeUnit.KBYTES, if (!sizeUnitsToExclude.contains(SizeUnit.BYTES)) kBytes.toLong().toDouble() else kBytes)
        result.putAll(sizeToMapStep(SizeUnit.KBYTES.toBytes(kBytes), s, sizeUnitsToExclude))
    } else if ((sizeUnitsToExclude.contains(SizeUnit.KBYTES) || s >= SizeUnit.C2 && s < SizeUnit.C3)
            && !sizeUnitsToExclude.contains(SizeUnit.MBYTES)) {
        val mBytes = SizeUnit.BYTES.toMBytes(s.toDouble())
        putIfNotNullOrZero(result, SizeUnit.MBYTES, if (!sizeUnitsToExclude.contains(SizeUnit.KBYTES)) mBytes.toLong().toDouble() else mBytes)
        result.putAll(sizeToMapStep(SizeUnit.MBYTES.toBytes(mBytes), s, sizeUnitsToExclude))
    } else if ((sizeUnitsToExclude.contains(SizeUnit.MBYTES) || s >= SizeUnit.C3)
            && !sizeUnitsToExclude.contains(SizeUnit.GBYTES)) {
        val gBytes = SizeUnit.BYTES.toGBytes(s.toDouble())
        putIfNotNullOrZero(result, SizeUnit.GBYTES, if (!sizeUnitsToExclude.contains(SizeUnit.MBYTES)) gBytes.toLong().toDouble() else gBytes)
        result.putAll(sizeToMapStep(SizeUnit.GBYTES.toBytes(gBytes), s, sizeUnitsToExclude))
    }
    return result
}

private fun sizeToMapStep(
        currentBytes: Long,
        sourceBytes: Long,
        sizeUnitsToExclude: Set<SizeUnit> = setOf()

): Map<SizeUnit, Double> {
    if (currentBytes > 0) {
        val restBytes = sourceBytes - currentBytes
        if (restBytes > 0) {
            return sizeToMap(restBytes.toDouble(), SizeUnit.BYTES, sizeUnitsToExclude)
        }
    }
    return emptyMap()
}
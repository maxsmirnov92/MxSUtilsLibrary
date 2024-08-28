package net.maxsmr.commonutils.conversion

import net.maxsmr.commonutils.collection.toSortedSetExclude
import net.maxsmr.commonutils.conversion.SizeUnit.Companion.SIZE_UNIT_BYTES_LARGEST
import net.maxsmr.commonutils.conversion.SizeUnit.Companion.SIZE_UNIT_BYTES_SMALLEST
import net.maxsmr.commonutils.conversion.SizeUnit.Companion.except
import net.maxsmr.commonutils.number.fraction
import net.maxsmr.commonutils.number.isGreater
import net.maxsmr.commonutils.number.isNotZero
import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.number.roundFormatted
import java.lang.Double.isInfinite
import java.lang.Double.isNaN
import java.math.BigDecimal

enum class SizeUnit {
    BYTES {
        override fun toBytes(s: Double): Double {
            return s
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

        override fun toTBytes(s: Double): Double {
            return s / C4
        }

        override fun toPBytes(s: Double): Double {
            return s / C5
        }
    },
    KBYTES {
        override fun toBytes(s: Double): Double {
            return s * C1
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

        override fun toTBytes(s: Double): Double {
            return s / C3
        }

        override fun toPBytes(s: Double): Double {
            return s / C4
        }

    },
    MBYTES {
        override fun toBytes(s: Double): Double {
            return s * C2
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

        override fun toTBytes(s: Double): Double {
            return s / C2
        }

        override fun toPBytes(s: Double): Double {
            return s / C3
        }
    },
    GBYTES {
        override fun toBytes(s: Double): Double {
            return s * C3
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

        override fun toTBytes(s: Double): Double {
            return s / C1
        }

        override fun toPBytes(s: Double): Double {
            return s / C2
        }
    },

    TBYTES {
        override fun toBytes(s: Double): Double {
            return s * C4
        }

        override fun toKBytes(s: Double): Double {
            return s * C3
        }

        override fun toMBytes(s: Double): Double {
            return s * C2
        }

        override fun toGBytes(s: Double): Double {
            return s * C1
        }

        override fun toTBytes(s: Double): Double {
            return s
        }

        override fun toPBytes(s: Double): Double {
            return s / C1
        }
    },

    PBYTES {

        override fun toBytes(s: Double): Double {
            return s * C5
        }

        override fun toKBytes(s: Double): Double {
            return s * C4
        }

        override fun toMBytes(s: Double): Double {
            return s * C3
        }

        override fun toGBytes(s: Double): Double {
            return s * C2
        }

        override fun toTBytes(s: Double): Double {
            return s * C2
        }

        override fun toPBytes(s: Double): Double {
            return s
        }
    };

    abstract fun toBytes(s: Double): Double
    abstract fun toKBytes(s: Double): Double
    abstract fun toMBytes(s: Double): Double
    abstract fun toGBytes(s: Double): Double
    abstract fun toTBytes(s: Double): Double
    abstract fun toPBytes(s: Double): Double

    fun toBits(s: Double): Long {
        return toBitsFromBytes(s)
    }

    fun toKBits(s: Double): Long {
        return toBitsFromBytes(toKBytes(s))
    }

    fun toMBits(s: Double): Long {
        return toBitsFromBytes(toMBytes(s))
    }

    fun toGBits(s: Double): Long {
        return toBitsFromBytes(toGBytes(s))
    }

    fun toTBits(s: Double): Long {
        return toBitsFromBytes(toTBytes(s))
    }

    fun toPBits(s: Double): Long {
        return toBitsFromBytes(toPBytes(s))
    }

    companion object {

        const val C0 = 8L
        const val C1 = 1024L
        const val C2 = C1 * 1024L
        const val C3 = C2 * 1024L
        const val C4 = C3 * 1024L
        const val C5 = C4 * 1024L

        val SIZE_UNIT_BYTES_LARGEST = PBYTES
        val SIZE_UNIT_BYTES_SMALLEST = BYTES

        fun toBitsFromBytes(s: Double): Long {
            return (s * C0).toLong()
        }

        fun convert(value: Number, from: SizeUnit, to: SizeUnit): Number {
            return when (to) {
                BYTES -> from.toBytes(value.toDouble())
                KBYTES -> from.toKBytes(value.toDouble())
                MBYTES -> from.toMBytes(value.toDouble())
                GBYTES -> from.toGBytes(value.toDouble())
                TBYTES -> from.toTBytes(value.toDouble())
                PBYTES -> from.toPBytes(value.toDouble())
            }
        }

        fun Collection<SizeUnit>.except(): Set<SizeUnit> {
            return if (isEmpty()) {
                SizeUnit.entries.toSet()
            } else {
                SizeUnit.entries.toSet() - this.toSet()
            }
        }
    }
}

enum class SizeUnitBits {

    BITS {
        override fun toBits(s: Long): Long {
            return s
        }

        override fun toKBits(s: Long): Long {
            return (s / SizeUnit.C1)
        }

        override fun toMBits(s: Long): Long {
            return (s / SizeUnit.C2)
        }

        override fun toGBits(s: Long): Long {
            return (s / SizeUnit.C3)
        }

        override fun toTBits(s: Long): Long {
            return (s / SizeUnit.C4)
        }

        override fun toPBits(s: Long): Long {
            return (s / SizeUnit.C5)
        }
    },
    KBITS {


        override fun toBits(s: Long): Long {
            return (s * SizeUnit.C1)
        }

        override fun toKBits(s: Long): Long {
            return s
        }

        override fun toMBits(s: Long): Long {
            return (s / SizeUnit.C1)
        }

        override fun toGBits(s: Long): Long {
            return (s / SizeUnit.C2)
        }

        override fun toTBits(s: Long): Long {
            return (s / SizeUnit.C3)
        }

        override fun toPBits(s: Long): Long {
            return (s / SizeUnit.C4)
        }
    },
    MBITS {


        override fun toBits(s: Long): Long {
            return (s * SizeUnit.C2)
        }

        override fun toKBits(s: Long): Long {
            return (s * SizeUnit.C1)
        }

        override fun toMBits(s: Long): Long {
            return s
        }

        override fun toGBits(s: Long): Long {
            return (s / SizeUnit.C1)
        }

        override fun toTBits(s: Long): Long {
            return (s / SizeUnit.C2)
        }

        override fun toPBits(s: Long): Long {
            return (s / SizeUnit.C3)
        }
    },
    GBITS {
        override fun toBits(s: Long): Long {
            return (s * SizeUnit.C3)
        }

        override fun toKBits(s: Long): Long {
            return (s * SizeUnit.C2)
        }

        override fun toMBits(s: Long): Long {
            return (s * SizeUnit.C1)
        }

        override fun toGBits(s: Long): Long {
            return s
        }

        override fun toTBits(s: Long): Long {
            return (s / SizeUnit.C1)
        }

        override fun toPBits(s: Long): Long {
            return (s / SizeUnit.C2)
        }
    },

    TBITS {
        override fun toBits(s: Long): Long {
            return (s * SizeUnit.C4)
        }

        override fun toKBits(s: Long): Long {
            return (s * SizeUnit.C3)
        }

        override fun toMBits(s: Long): Long {
            return (s * SizeUnit.C2)
        }

        override fun toGBits(s: Long): Long {
            return (s * SizeUnit.C1)
        }

        override fun toTBits(s: Long): Long {
            return s
        }

        override fun toPBits(s: Long): Long {
            return (s / SizeUnit.C1)
        }
    },

    PBITS {
        override fun toBits(s: Long): Long {
            return (s * SizeUnit.C5)
        }

        override fun toKBits(s: Long): Long {
            return (s * SizeUnit.C4)
        }

        override fun toMBits(s: Long): Long {
            return (s * SizeUnit.C3)
        }

        override fun toGBits(s: Long): Long {
            return (s * SizeUnit.C2)
        }

        override fun toTBits(s: Long): Long {
            return (s * SizeUnit.C1)
        }

        override fun toPBits(s: Long): Long {
            return s
        }
    };

    abstract fun toBits(s: Long): Long
    abstract fun toKBits(s: Long): Long
    abstract fun toMBits(s: Long): Long
    abstract fun toGBits(s: Long): Long
    abstract fun toTBits(s: Long): Long
    abstract fun toPBits(s: Long): Long

    fun toBytes(s: Long): Double {
        return toBytesFromBits(s)
    }

    fun toKBytes(s: Long): Double {
        return toBytesFromBits(toKBits(s))
    }

    fun toMBytes(s: Long): Double {
        return toBytesFromBits(toMBits(s))
    }

    fun toGBytes(s: Long): Double {
        return toBytesFromBits(toGBits(s))
    }

    fun toTBytes(s: Long): Double {
        return toBytesFromBits(toTBits(s))
    }

    fun toPBytes(s: Long): Double {
        return toBytesFromBits(toPBits(s))
    }

    companion object {

        val SIZE_UNIT_BITS_LARGEST = PBITS
        val SIZE_UNIT_BITS_SMALLEST = BITS

        fun toBytesFromBits(s: Long): Double {
            return s.toDouble() / SizeUnit.C0
        }

        fun convert(value: Number, from: SizeUnitBits, to: SizeUnitBits): Number {
            return when (to) {
                BITS -> from.toBits(value.toLong())
                KBITS -> from.toKBits(value.toLong())
                MBITS -> from.toMBits(value.toLong())
                GBITS -> from.toGBits(value.toLong())
                TBITS -> from.toTBits(value.toLong())
                PBITS -> from.toPBits(value.toLong())
            }
        }

        fun Collection<SizeUnitBits>.except(): Set<SizeUnitBits> {
            return if (isEmpty()) {
                SizeUnitBits.entries.toSet()
            } else {
                SizeUnitBits.entries.toSet() - this.toSet()
            }
        }
    }
}

/**
 * @param size неотрицательный размер в [sizeUnit]
 * @param sizeUnitsToExclude единицы, нежелательные в итоговом результате
 * @param ignoreExclusionIfOnly если true и проверяемая единица единственная и находится в исключениях - будет включено в результат
 * (если в исключениях также более мелкая за ней - попадёт эта, более крупная)
 * @param precision кол-во знаков после запятой: null - оставить получившийся Double, 0 - без дробной части
 * @param singleResult true, если в результате должен быть единственный [SizeUnit], округлённый в соот-ии с precision
 * @param emptyMapIfZero если false, при пустом результате будет дописан 0 с минимальной единицей
 * @return мапа: единица измерения + количество
 */
@JvmOverloads
fun decomposeSize(
    size: Number,
    sizeUnit: SizeUnit,
    sizeUnitsToExclude: Set<SizeUnit> = setOf(),
    ignoreExclusionIfOnly: Boolean = true,
    precision: Int? = 0,
    singleResult: Boolean = false,
    emptyMapIfZero: Boolean = true,
): Map<SizeUnit, Number> {

    /**
     * @return ненулевой Double/Long или null
     */
    fun Number.roundOrNull(hasSmaller: Boolean, precision: Int?): Number? {
        return if (this is Long) {
            this.takeIf { it != 0L }
        } else {
            val fraction = this.toBigDecimal().fraction()
            // у этой отбрасываем дробную часть, если:
            if (precision == 0 // целевое число знаков после запятой 0
                || fraction.isZero() // или дробная часть 0
                || fraction.isGreater(BigDecimal.ZERO) && hasSmaller // или есть следующая по мелкости
            ) {
                this.toLong().takeIf { it != 0L }
            } else {
                val value = this.toDouble()
                (if (precision == null) {
                    // без изменений
                    value
                } else {
                    // округление через String.format с точностью precision
                    value.roundFormatted(precision)
                }).takeIf { it.isNotZero() }
            }
        }
    }

    val result = decomposeSize(size, sizeUnit, ignoreExclusionIfOnly, sizeUnitsToExclude, setOf()).toMutableMap()
    if (singleResult && result.size > 1) {
        val largestUnit = result.keys.first()
        val value = SizeUnit.convert(size, sizeUnit, largestUnit)
        result.clear()
        result[largestUnit] = value
    }

    // после финального шага округление по всем в соот-ии с precision
    val iterator = result.toMap().iterator()
    result.clear()
    while (iterator.hasNext()) {
        val current = iterator.next()
        current.value.roundOrNull(iterator.hasNext(), precision)?.let {
            result[current.key] = it
        }
    }

    if (!emptyMapIfZero && result.isEmpty()) {
        val allowedSmallestUnit = if (sizeUnitsToExclude.contains(SIZE_UNIT_BYTES_SMALLEST)) {
            sizeUnitsToExclude.except().minOfOrNull {
                SizeUnit.entries.indexOf(it)
            }?.let {
                SizeUnit.entries[it]
            }
        } else {
            SIZE_UNIT_BYTES_SMALLEST
        }
        allowedSmallestUnit?.let {
            // при пустой мапе докидываем 0
            result[it] = 0
        }
    }
    return result
}

/**
 * @param alreadyDecomposedUnits единицы, уже включённые в результат
 */
private fun decomposeSize(
    size: Number,
    sizeUnit: SizeUnit,
    ignoreExclusionIfOnly: Boolean,
    sizeUnitsToExclude: Set<SizeUnit>,
    alreadyDecomposedUnits: Set<SizeUnit>,
): Map<SizeUnit, Number> {
    val size = size.toDouble()
    if (size < 0 || isInfinite(size) || isNaN(size)) return mapOf()

    val s = sizeUnit.toBytes(size)

    fun SizeUnit.isInRange() = when (this) {
        SizeUnit.PBYTES -> s >= SizeUnit.C5
        SizeUnit.TBYTES -> s >= SizeUnit.C4 && s < SizeUnit.C5
        SizeUnit.GBYTES -> s >= SizeUnit.C3 && s < SizeUnit.C4
        SizeUnit.MBYTES -> s >= SizeUnit.C2 && s < SizeUnit.C3
        SizeUnit.KBYTES -> s >= SizeUnit.C1 && s < SizeUnit.C2
        SizeUnit.BYTES -> s < SizeUnit.C1
    }

    fun SizeUnit.isInRangeOrBiggerExcluded(): Boolean {
        if (isInRange()) {
            // эта находится в диапазоне - необходимое и достаточное условие
            return true
        }
        if (this != SIZE_UNIT_BYTES_LARGEST) {
            // следующая по крупности от этой единица проверяется на вхождение в исключения
            val biggerUnit = SizeUnit.entries.getOrNull(this.ordinal + 1)
            return biggerUnit != null && sizeUnitsToExclude.contains(biggerUnit)
        }
        return false
    }

    fun SizeUnit.checkAcceptable(): Boolean {
        if (!this.isInRangeOrBiggerExcluded()) {
            return false
        }
        val smallerUnits = SizeUnit.entries.filter {
            // более мелкая единица по отношению к этой, не в исключениях
            if (!(it.ordinal < this.ordinal
                        && !sizeUnitsToExclude.contains(it))
            ) {
                return@filter false
            }
            // текущее значение попадает в диапазон предыдущей единицы
            val smallerUnit = SizeUnit.entries.getOrNull(this.ordinal - 1)
            smallerUnit?.isInRangeOrBiggerExcluded() ?: false
        }
        // данная единица не в исключениях - необходимое и достаточное условие
        return !sizeUnitsToExclude.contains(this)
                // ИЛИ она в исключениях, НО:
                || ignoreExclusionIfOnly &&
                ( // отсутствуют: ранее заполненные
                        alreadyDecomposedUnits.isEmpty()
                                // И более мелкие единицы НЕ в исключениях
                                // для попадания в мапу на следующих decomposeTime
                                && smallerUnits.isEmpty()
                        )
    }

    val result = sortedMapOf<SizeUnit, Number>()

    if (SizeUnit.PBYTES.checkAcceptable()) {
        val pBytes = SizeUnit.BYTES.toPBytes(s)
        val pBytesLong = pBytes.toLong().toDouble()
        if (pBytes.isNotZero()) {
            result[SizeUnit.PBYTES] = pBytes
        }
        result.putAll(
            decomposeSizeStep(
                SizeUnit.PBYTES.toBytes(pBytesLong),
                s,
                sizeUnitsToExclude,
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (SizeUnit.TBYTES.checkAcceptable()) {
        val tBytes = SizeUnit.BYTES.toTBytes(s)
        val tBytesLong = tBytes.toLong().toDouble()
        if (tBytes.isNotZero()) {
            result[SizeUnit.TBYTES] = tBytes
        }
        result.putAll(
            decomposeSizeStep(
                SizeUnit.TBYTES.toBytes(tBytesLong),
                s,
                sizeUnitsToExclude,
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (SizeUnit.GBYTES.checkAcceptable()) {
        val gBytes = SizeUnit.BYTES.toGBytes(s)
        val gBytesLong = gBytes.toLong().toDouble()
        if (gBytes.isNotZero()) {
            result[SizeUnit.GBYTES] = gBytes
        }
        result.putAll(
            decomposeSizeStep(
                SizeUnit.GBYTES.toBytes(gBytesLong),
                s,
                sizeUnitsToExclude,
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (SizeUnit.MBYTES.checkAcceptable()) {
        val mBytes = SizeUnit.BYTES.toMBytes(s)
        val mBytesLong = mBytes.toLong().toDouble()
        if (mBytes.isNotZero()) {
            result[SizeUnit.MBYTES] = mBytes
        }
        result.putAll(
            decomposeSizeStep(
                SizeUnit.MBYTES.toBytes(mBytesLong),
                s,
                sizeUnitsToExclude.toSortedSetExclude(setOf(SizeUnit.GBYTES)),
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (SizeUnit.KBYTES.checkAcceptable()) {
        val kBytes = SizeUnit.BYTES.toKBytes(s)
        val kBytesLong = kBytes.toLong().toDouble()
        if (kBytes.isNotZero()) {
            result[SizeUnit.KBYTES] = kBytes
        }
        result.putAll(
            decomposeSizeStep(
                SizeUnit.KBYTES.toBytes(kBytesLong),
                s,
                sizeUnitsToExclude.toSortedSetExclude(setOf(SizeUnit.MBYTES)),
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (SizeUnit.BYTES.checkAcceptable()) {
        if (s.isNotZero()) {
            result[SizeUnit.BYTES] = s
        }
    }

    // порядок сейчас от мелких к крупным - возвращаем результат в обратном порядке по ordinal
    return result.toSortedMap(reverseOrder())
}

private fun decomposeSizeStep(
    currentBytes: Double,
    sourceBytes: Double,
    sizeUnitsToExclude: Set<SizeUnit>,
    ignoreExclusionIfOnly: Boolean,
    alreadyDecomposedUnits: Set<SizeUnit>,
): Map<SizeUnit, Number> {
    if (currentBytes > 0) {
        val restBytes = sourceBytes - currentBytes
        if (restBytes > 0) {
            return decomposeSize(
                restBytes,
                SizeUnit.BYTES,
                ignoreExclusionIfOnly,
                sizeUnitsToExclude,
                alreadyDecomposedUnits,
            )
        }
    }
    return emptyMap()
}


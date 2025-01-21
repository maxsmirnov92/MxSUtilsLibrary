package net.maxsmr.commonutils.conversion

import net.maxsmr.commonutils.collection.toSortedSetExclude
import net.maxsmr.commonutils.conversion.CountUnit.Companion.COUNT_UNIT_LARGEST
import net.maxsmr.commonutils.conversion.CountUnit.Companion.COUNT_UNIT_SMALLEST
import net.maxsmr.commonutils.conversion.CountUnit.Companion.except
import net.maxsmr.commonutils.number.fraction
import net.maxsmr.commonutils.number.isGreater
import net.maxsmr.commonutils.number.isNotZero
import net.maxsmr.commonutils.number.isZero
import net.maxsmr.commonutils.number.roundFormatted
import java.lang.Double.isInfinite
import java.lang.Double.isNaN
import java.math.BigDecimal

enum class CountUnit {
    UNITS {

        override fun toUnits(s: Double): Double {
            return s
        }

        override fun toKUnits(s: Double): Double {
            return s / C1
        }

        override fun toMUnits(s: Double): Double {
            return s / C2
        }

        override fun toGUnits(s: Double): Double {
            return s / C3
        }

        override fun toTUnits(s: Double): Double {
            return s / C4
        }

        override fun toPUnits(s: Double): Double {
            return s / C5
        }
    },
    
    K_UNITS {

        override fun toUnits(s: Double): Double {
            return s * C1
        }

        override fun toKUnits(s: Double): Double {
            return s
        }

        override fun toMUnits(s: Double): Double {
            return s / C1
        }

        override fun toGUnits(s: Double): Double {
            return s / C2
        }

        override fun toTUnits(s: Double): Double {
            return s / C3
        }

        override fun toPUnits(s: Double): Double {
            return s / C4
        }
    },

    M_UNITS {

        override fun toUnits(s: Double): Double {
            return s * C2
        }

        override fun toKUnits(s: Double): Double {
            return s * C1
        }

        override fun toMUnits(s: Double): Double {
            return s
        }

        override fun toGUnits(s: Double): Double {
            return s / C1
        }

        override fun toTUnits(s: Double): Double {
            return s / C2
        }

        override fun toPUnits(s: Double): Double {
            return s / C3
        }
    },

    G_UNITS {

        override fun toUnits(s: Double): Double {
            return s * C3
        }

        override fun toKUnits(s: Double): Double {
            return s * C2
        }

        override fun toMUnits(s: Double): Double {
            return s  * C1
        }

        override fun toGUnits(s: Double): Double {
            return s
        }

        override fun toTUnits(s: Double): Double {
            return s / C1
        }

        override fun toPUnits(s: Double): Double {
            return s / C2
        }
    },

    T_UNITS {

        override fun toUnits(s: Double): Double {
            return s * C4
        }

        override fun toKUnits(s: Double): Double {
            return s * C3
        }

        override fun toMUnits(s: Double): Double {
            return s  * C2
        }

        override fun toGUnits(s: Double): Double {
            return s * C1
        }

        override fun toTUnits(s: Double): Double {
            return s
        }

        override fun toPUnits(s: Double): Double {
            return s / C1
        }
    },

    P_UNITS {

        override fun toUnits(s: Double): Double {
            return s * C5
        }

        override fun toKUnits(s: Double): Double {
            return s * C4
        }

        override fun toMUnits(s: Double): Double {
            return s  * C3
        }

        override fun toGUnits(s: Double): Double {
            return s * C2
        }

        override fun toTUnits(s: Double): Double {
            return s * C1
        }

        override fun toPUnits(s: Double): Double {
            return s
        }
    };

    abstract fun toUnits(s: Double): Double
    abstract fun toKUnits(s: Double): Double
    abstract fun toMUnits(s: Double): Double
    abstract fun toGUnits(s: Double): Double
    abstract fun toTUnits(s: Double): Double
    abstract fun toPUnits(s: Double): Double
    
    companion object {

        const val C0 = 1
        const val C1 = C0 * 1000L
        const val C2 = C1 * 1000L
        const val C3 = C2 * 1000L
        const val C4 = C3 * 1000L
        const val C5 = C4 * 1000L

        val COUNT_UNIT_LARGEST = P_UNITS
        val COUNT_UNIT_SMALLEST = UNITS

        fun convert(value: Number, from: CountUnit, to: CountUnit): Number {
            return when (to) {
                UNITS -> from.toUnits(value.toDouble())
                K_UNITS -> from.toKUnits(value.toDouble())
                M_UNITS -> from.toMUnits(value.toDouble())
                G_UNITS -> from.toGUnits(value.toDouble())
                T_UNITS -> from.toTUnits(value.toDouble())
                P_UNITS -> from.toPUnits(value.toDouble())
            }
        }

        fun Collection<CountUnit>.except(): Set<CountUnit> {
            return if (isEmpty()) {
                CountUnit.entries.toSet()
            } else {
                CountUnit.entries.toSet() - this.toSet()
            }
        }
    }
}

/**
 * @param count неотрицательное кол-во в [countUnit]
 * @param countUnitsToExclude единицы, нежелательные в итоговом результате
 * @param ignoreExclusionIfOnly если true и проверяемая единица единственная и находится в исключениях - будет включено в результат
 * (если в исключениях также более мелкая за ней - попадёт эта, более крупная)
 * @param precision кол-во знаков после запятой: null - оставить получившийся Double, 0 - без дробной части
 * @param singleResult true, если в результате должен быть единственный [CountUnit], округлённый в соот-ии с precision
 * @param emptyMapIfZero если false, при пустом результате будет дописан 0 с минимальной единицей
 * @return мапа: единица измерения + количество
 */
@JvmOverloads
fun decomposeCount(
    count: Number,
    countUnit: CountUnit,
    countUnitsToExclude: Set<CountUnit> = setOf(),
    ignoreExclusionIfOnly: Boolean = true,
    precision: Int? = 0,
    singleResult: Boolean = false,
    emptyMapIfZero: Boolean = true,
): Map<CountUnit, Number> {

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

    val result = decomposeCount(count, countUnit, ignoreExclusionIfOnly, countUnitsToExclude, setOf()).toMutableMap()
    if (singleResult && result.size > 1) {
        val largestUnit = result.keys.first()
        val value = CountUnit.convert(count, countUnit, largestUnit)
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
        val allowedSmallestUnit = if (countUnitsToExclude.contains(COUNT_UNIT_SMALLEST)) {
            countUnitsToExclude.except().minOfOrNull {
                CountUnit.entries.indexOf(it)
            }?.let {
                CountUnit.entries[it]
            }
        } else {
            COUNT_UNIT_SMALLEST
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
private fun decomposeCount(
    count: Number,
    countUnit: CountUnit,
    ignoreExclusionIfOnly: Boolean,
    countUnitsToExclude: Set<CountUnit>,
    alreadyDecomposedUnits: Set<CountUnit>,
): Map<CountUnit, Number> {
    val count = count.toDouble()
    if (count < 0 || isInfinite(count) || isNaN(count)) return mapOf()

    val s = countUnit.toUnits(count)

    fun CountUnit.isInRange() = when (this) {
        CountUnit.P_UNITS -> s >= CountUnit.C5
        CountUnit.T_UNITS -> s >= CountUnit.C4 && s < CountUnit.C5
        CountUnit.G_UNITS -> s >= CountUnit.C3 && s < CountUnit.C4
        CountUnit.M_UNITS -> s >= CountUnit.C2 && s < CountUnit.C3
        CountUnit.K_UNITS -> s >= CountUnit.C1 && s < CountUnit.C2
        CountUnit.UNITS -> s < CountUnit.C1
    }

    fun CountUnit.isInRangeOrBiggerExcluded(): Boolean {
        if (isInRange()) {
            // эта находится в диапазоне - необходимое и достаточное условие
            return true
        }
        if (this != COUNT_UNIT_LARGEST) {
            // следующая по крупности от этой единица проверяется на вхождение в исключения
            val biggerUnit = CountUnit.entries.getOrNull(this.ordinal + 1)
            return biggerUnit != null && countUnitsToExclude.contains(biggerUnit)
        }
        return false
    }

    fun CountUnit.checkAcceptable(): Boolean {
        if (!this.isInRangeOrBiggerExcluded()) {
            return false
        }
        val smallerUnits = CountUnit.entries.filter {
            // более мелкая единица по отношению к этой, не в исключениях
            if (!(it.ordinal < this.ordinal
                        && !countUnitsToExclude.contains(it))
            ) {
                return@filter false
            }
            // текущее значение попадает в диапазон предыдущей единицы
            val smallerUnit = CountUnit.entries.getOrNull(this.ordinal - 1)
            smallerUnit?.isInRangeOrBiggerExcluded() ?: false
        }
        // данная единица не в исключениях - необходимое и достаточное условие
        return !countUnitsToExclude.contains(this)
                // ИЛИ она в исключениях, НО:
                || ignoreExclusionIfOnly &&
                ( // отсутствуют: ранее заполненные
                        alreadyDecomposedUnits.isEmpty()
                                // И более мелкие единицы НЕ в исключениях
                                // для попадания в мапу на следующих decomposeTime
                                && smallerUnits.isEmpty()
                        )
    }

    val result = sortedMapOf<CountUnit, Number>()

    if (CountUnit.P_UNITS.checkAcceptable()) {
        val pUnits = CountUnit.UNITS.toPUnits(s)
        val pUnitsLong = pUnits.toLong().toDouble()
        if (pUnits.isNotZero()) {
            result[CountUnit.P_UNITS] = pUnits
        }
        result.putAll(
            decomposeCountStep(
                CountUnit.P_UNITS.toUnits(pUnitsLong),
                s,
                countUnitsToExclude,
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (CountUnit.T_UNITS.checkAcceptable()) {
        val tUnits = CountUnit.UNITS.toTUnits(s)
        val tUnitsLong = tUnits.toLong().toDouble()
        if (tUnits.isNotZero()) {
            result[CountUnit.T_UNITS] = tUnits
        }
        result.putAll(
            decomposeCountStep(
                CountUnit.T_UNITS.toUnits(tUnitsLong),
                s,
                countUnitsToExclude,
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (CountUnit.G_UNITS.checkAcceptable()) {
        val gUnits = CountUnit.UNITS.toGUnits(s)
        val gUnitsLong = gUnits.toLong().toDouble()
        if (gUnits.isNotZero()) {
            result[CountUnit.G_UNITS] = gUnits
        }
        result.putAll(
            decomposeCountStep(
                CountUnit.G_UNITS.toUnits(gUnitsLong),
                s,
                countUnitsToExclude,
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (CountUnit.M_UNITS.checkAcceptable()) {
        val mUnits = CountUnit.UNITS.toMUnits(s)
        val mUnitsLong = mUnits.toLong().toDouble()
        if (mUnits.isNotZero()) {
            result[CountUnit.M_UNITS] = mUnits
        }
        result.putAll(
            decomposeCountStep(
                CountUnit.M_UNITS.toUnits(mUnitsLong),
                s,
                countUnitsToExclude.toSortedSetExclude(setOf(CountUnit.G_UNITS)),
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (CountUnit.K_UNITS.checkAcceptable()) {
        val kUnits = CountUnit.UNITS.toKUnits(s)
        val kUnitsLong = kUnits.toLong().toDouble()
        if (kUnits.isNotZero()) {
            result[CountUnit.K_UNITS] = kUnits
        }
        result.putAll(
            decomposeCountStep(
                CountUnit.K_UNITS.toUnits(kUnitsLong),
                s,
                countUnitsToExclude.toSortedSetExclude(setOf(CountUnit.M_UNITS)),
                ignoreExclusionIfOnly,
                result.keys,
            )
        )
    } else if (CountUnit.UNITS.checkAcceptable()) {
        if (s.isNotZero()) {
            result[CountUnit.UNITS] = s
        }
    }

    // порядок сейчас от мелких к крупным - возвращаем результат в обратном порядке по ordinal
    return result.toSortedMap(reverseOrder())
}

private fun decomposeCountStep(
    currentUnits: Double,
    sourceUnits: Double,
    countUnitsToExclude: Set<CountUnit>,
    ignoreExclusionIfOnly: Boolean,
    alreadyDecomposedUnits: Set<CountUnit>,
): Map<CountUnit, Number> {
    if (currentUnits > 0) {
        val restUnits = sourceUnits - currentUnits
        if (restUnits > 0) {
            return decomposeCount(
                restUnits,
                CountUnit.UNITS,
                ignoreExclusionIfOnly,
                countUnitsToExclude,
                alreadyDecomposedUnits,
            )
        }
    }
    return emptyMap()
}

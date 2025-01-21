package net.maxsmr.commonutils.format

import androidx.annotation.StringRes
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.conversion.CountUnit
import net.maxsmr.commonutils.conversion.decomposeCount
import net.maxsmr.commonutils.gui.message.TextMessage

@JvmOverloads
fun decomposeCountFormatted(
    count: Number,
    countUnit: CountUnit,
    countUnitsToExclude: Set<CountUnit> = setOf(),
    ignoreExclusionIfOnly: Boolean = true,
    precision: Int? = 0,
    singleResult: Boolean = false,
    emptyIfZero: Boolean = true,
    formatWithValue: Boolean = true,
): List<TextMessage> {
    return decomposeCountFormatted(
        count,
        countUnit,
        countUnitsToExclude,
        ignoreExclusionIfOnly,
        precision,
        singleResult,
        emptyIfZero,
        formatWithValue
    ) { resId, value ->
        toTextMessage(resId, value, formatWithValue)
    }
}

fun decomposeCountFormatted(
    decomposedMap: Map<CountUnit, Number>,
    formatWithValue: Boolean,
): List<TextMessage> {
    return decomposeCountFormatted(decomposedMap, formatWithValue) { resId, value ->
        toTextMessage(resId, value, formatWithValue)
    }
}

fun <F> decomposeCountFormatted(
    count: Number,
    countUnit: CountUnit,
    countUnitsToExclude: Set<CountUnit> = setOf(),
    ignoreExclusionIfOnly: Boolean = true,
    precision: Int? = 0,
    singleResult: Boolean = false,
    emptyIfZero: Boolean = true,
    formatWithValue: Boolean = true,
    formatFunc: (Int?, Number) -> F,
): List<F> {
    val map = decomposeCount(count, countUnit, countUnitsToExclude, ignoreExclusionIfOnly, precision, singleResult, emptyIfZero)
    return decomposeCountFormatted(map, formatWithValue, formatFunc)
}

fun <F> decomposeCountFormatted(
    decomposedMap: Map<CountUnit, Number>,
    formatWithValue: Boolean,
    formatFunc: (Int?, Number) -> F,
): List<F> {
    val result = mutableListOf<F>()
    decomposedMap.forEach {
        val textResId = getTextResId(it.key, formatWithValue)
        result.add(formatFunc(textResId, it.value))
    }
    return result
}

@JvmOverloads
fun formatCountSingle(
    count: Number,
    countUnit: CountUnit,
    countUnitsToExclude: Set<CountUnit> = setOf(),
    precision: Int? = 0,
    emptyIfZero: Boolean = true,
): TextMessage? {
    return formatCountSingle(
        decomposeCount(
            count,
            countUnit,
            countUnitsToExclude,
            true,
            precision,
            true,
            emptyIfZero
        )
    )
}

fun formatCountSingle(
    decomposedMap: Map<CountUnit, Number>,
): TextMessage? {
    decomposedMap.toList().firstOrNull()?.let {
        return decomposeCountFormatted(mapOf(it), true).firstOrNull()
    }
    return null
}

private fun toTextMessage(
    @StringRes resId: Int?,
    value: Number,
    formatWithValue: Boolean,
): TextMessage {
    return if (resId == null) {
        TextMessage(value.toString())
    } else {
        if (formatWithValue) {
            TextMessage(resId, value)
        } else {
            TextMessage(resId)
        }
    }
}

@StringRes
private fun getTextResId(countUnit: CountUnit, isWithValue: Boolean): Int? {
    return when (countUnit) {
        CountUnit.UNITS -> {
            null
        }

        CountUnit.K_UNITS -> {
            if (isWithValue) {
                R.string.count_unit_k_format
            } else {
                R.string.count_unit_k
            }
        }

        CountUnit.M_UNITS -> {
            if (isWithValue) {
                R.string.count_unit_m_format
            } else {
                R.string.count_unit_m
            }
        }

        CountUnit.G_UNITS -> {
            if (isWithValue) {
                R.string.count_unit_g_format
            } else {
                R.string.count_unit_g
            }
        }

        CountUnit.T_UNITS -> {
            if (isWithValue) {
                R.string.count_unit_t_format
            } else {
                R.string.count_unit_t
            }
        }

        CountUnit.P_UNITS -> {
            if (isWithValue) {
                R.string.count_unit_p_format
            } else {
                R.string.count_unit_p
            }
        }
    }
}
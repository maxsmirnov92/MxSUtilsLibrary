package net.maxsmr.commonutils.format

import android.content.Context
import androidx.annotation.PluralsRes
import net.maxsmr.commonutils.Pair
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.conversion.SizeUnit
import net.maxsmr.commonutils.conversion.SizeUnitBits
import net.maxsmr.commonutils.conversion.decomposeSize
import net.maxsmr.commonutils.getSize
import net.maxsmr.commonutils.gui.message.PluralTextMessage
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.text.NEXT_LINE
import net.maxsmr.commonutils.text.join
import java.io.File
import java.util.concurrent.TimeUnit

private const val FORMAT_FILE_SIZE = "%s: %s"

fun filePairsToString(
    context: Context,
    files: Collection<Pair<File, File>>,
    depth: Int,
    sizeUnitsToExclude: Set<SizeUnit> = setOf(),
    precision: Int? = 0,
): String {
    val map = mutableMapOf<Pair<File, File>, Long>()
    for (p in files) {
        if (p.first != null) {
            map[p] = getSize(p.first, depth)
        }
    }
    return filePairsWithSizeToString(context, map, sizeUnitsToExclude, precision)
}

/**
 * @param files pair < source file - destination file > <-> size in bytes
 */
fun filePairsWithSizeToString(
    context: Context,
    files: Map<Pair<File, File>, Long>,
    sizeUnitsToExclude: Set<SizeUnit> = setOf(),
    precision: Int? = 0,
): String {
    val result = mutableListOf<String>()
    for (f in files) {
        val key = f.key
        val sourceFile = key.first
        val destinationFile = key.second
        if (sourceFile != null) {
            val sb = StringBuilder()
            sb.append(sourceFile.absolutePath)
            if (destinationFile != null) {
                sb.append(" -> ")
                sb.append(destinationFile.absolutePath)
            }
            val size = f.value
            result.add(
                String.format(
                    FORMAT_FILE_SIZE,
                    sb.toString(),
                    decomposeSizeFormatted(
                        size.toDouble(),
                        SizeUnit.BYTES,
                        sizeUnitsToExclude,
                        precision = precision,
                        formatWithValue = true,
                    ).joinToString { it.get(context) }
                )
            )
        }
    }
    return join(NEXT_LINE, result)
}

@JvmOverloads
fun decomposeSizeFormatted(
    size: Number,
    sizeUnit: SizeUnit,
    sizeUnitsToExclude: Set<SizeUnit> = setOf(),
    ignoreExclusionIfOnly: Boolean = true,
    precision: Int? = 0,
    singleResult: Boolean = false,
    emptyIfZero: Boolean = true,
    formatWithValue: Boolean,
): List<PluralTextMessage> {
    return decomposeSizeFormatted(
        size,
        sizeUnit,
        sizeUnitsToExclude,
        ignoreExclusionIfOnly,
        precision,
        singleResult,
        emptyIfZero,
        formatWithValue
    ) { resId, value ->
        toPluralTextMessage(resId, value.toLong(), formatWithValue)
    }
}

fun decomposeSizeFormatted(
    decomposedMap: Map<SizeUnit, Number>,
    formatWithValue: Boolean,
): List<PluralTextMessage> {
    return decomposeSizeFormatted(decomposedMap, formatWithValue) { resId, value ->
        toPluralTextMessage(resId, value.toLong(), formatWithValue)
    }
}

fun <F> decomposeSizeFormatted(
    size: Number,
    sizeUnit: SizeUnit,
    sizeUnitsToExclude: Set<SizeUnit> = setOf(),
    ignoreExclusionIfOnly: Boolean = true,
    precision: Int? = 0,
    singleResult: Boolean = false,
    emptyIfZero: Boolean = true,
    formatWithValue: Boolean,
    formatFunc: (Int, Number) -> F,
): List<F> {
    val map = decomposeSize(size, sizeUnit, sizeUnitsToExclude, ignoreExclusionIfOnly, precision, singleResult, emptyIfZero)
    return decomposeSizeFormatted(map, formatWithValue, formatFunc)
}

fun <F> decomposeSizeFormatted(
    decomposedMap: Map<SizeUnit, Number>,
    formatWithValue: Boolean,
    formatFunc: (Int, Number) -> F,
): List<F> {
    val result = mutableListOf<F>()
    decomposedMap.forEach {
        val pluralResId = getPluralTextResId(it.key, formatWithValue)
        result.add(formatFunc(pluralResId, it.value))
    }
    return result
}

@JvmOverloads
fun formatSpeedSize(
    size: Number,
    sizeUnit: SizeUnit,
    sizeUnitsToExclude: Set<SizeUnit> = setOf(),
    precision: Int? = 0,
    emptyIfZero: Boolean = true,
    timeUnit: TimeUnit,
): TextMessage? {
    return formatSpeedSize(
        decomposeSize(size,
            sizeUnit,
            sizeUnitsToExclude,
            true,
            precision,
            true,
            emptyIfZero
        ), timeUnit)
}

fun formatSpeedSize(
    decomposedMap: Map<SizeUnit, Number>,
    timeUnit: TimeUnit,
): TextMessage? {
    decomposedMap.toList().firstOrNull()?.let {
        decomposeSizeFormatted(mapOf(it), false).firstOrNull()?.let { message ->
            return message.toSpeedTextMessage(it.second, timeUnit)
        }
    }
    return null
}

private fun PluralTextMessage.toSpeedTextMessage(value: Number, timeUnit: TimeUnit): TextMessage {
    val messageResId = when (timeUnit) {
        TimeUnit.NANOSECONDS -> R.string.speed_nanos_format
        TimeUnit.MICROSECONDS -> R.string.speed_micros_format
        TimeUnit.MILLISECONDS -> R.string.speed_millis_format
        TimeUnit.SECONDS -> R.string.speed_seconds_format
        TimeUnit.MINUTES -> R.string.speed_minutes_format
        TimeUnit.HOURS -> R.string.speed_hours_format
        TimeUnit.DAYS -> R.string.speed_days_format
    }
    return TextMessage(messageResId, value.toString(), this)
}

@PluralsRes
private fun getPluralTextResId(sizeUnit: SizeUnit, isWithValue: Boolean): Int {
    return when (sizeUnit) {
        SizeUnit.BYTES -> {
            if (isWithValue) {
                R.plurals.size_unit_bytes_format
            } else {
                R.plurals.size_unit_bytes
            }
        }

        SizeUnit.KBYTES -> {
            if (isWithValue) {
                R.plurals.size_unit_kbytes_format
            } else {
                R.plurals.size_unit_kbytes
            }
        }

        SizeUnit.MBYTES -> {
            if (isWithValue) {
                R.plurals.size_unit_mbytes_format
            } else {
                R.plurals.size_unit_mbytes
            }
        }

        SizeUnit.GBYTES -> {
            if (isWithValue) {
                R.plurals.size_unit_gbytes_format
            } else {
                R.plurals.size_unit_gbytes
            }
        }

        SizeUnit.TBYTES -> {
            if (isWithValue) {
                R.plurals.size_unit_tbytes_format
            } else {
                R.plurals.size_unit_tbytes
            }
        }

        SizeUnit.PBYTES -> {
            if (isWithValue) {
                R.plurals.size_unit_pbytes_format
            } else {
                R.plurals.size_unit_pbytes
            }
        }
    }
}

@PluralsRes
private fun getPluralTextResId(sizeUnit: SizeUnitBits, isWithValue: Boolean): Int {
    return when (sizeUnit) {
        SizeUnitBits.BITS -> {
            if (isWithValue) {
                R.plurals.size_unit_bits_format
            } else {
                R.plurals.size_unit_bits
            }
        }

        SizeUnitBits.KBITS -> {
            if (isWithValue) {
                R.plurals.size_unit_kbits_format
            } else {
                R.plurals.size_unit_kbits
            }
        }

        SizeUnitBits.MBITS -> {
            if (isWithValue) {
                R.plurals.size_unit_mbits_format
            } else {
                R.plurals.size_unit_mbits
            }
        }

        SizeUnitBits.GBITS -> {
            if (isWithValue) {
                R.plurals.size_unit_gbits_format
            } else {
                R.plurals.size_unit_gbits
            }
        }

        SizeUnitBits.TBITS -> {
            if (isWithValue) {
                R.plurals.size_unit_tbits_format
            } else {
                R.plurals.size_unit_tbits
            }
        }

        SizeUnitBits.PBITS -> {
            if (isWithValue) {
                R.plurals.size_unit_pbits_format
            } else {
                R.plurals.size_unit_pbits
            }
        }
    }
}
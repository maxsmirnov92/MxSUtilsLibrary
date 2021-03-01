package net.maxsmr.commonutils.format

import android.content.Context
import net.maxsmr.commonutils.Pair
import net.maxsmr.commonutils.R
import net.maxsmr.commonutils.conversion.SizeUnit
import net.maxsmr.commonutils.conversion.sizeToMap
import net.maxsmr.commonutils.getSize
import net.maxsmr.commonutils.text.NEXT_LINE
import net.maxsmr.commonutils.text.join
import java.io.File
import java.util.*

private const val FORMAT_SIZE = "%s %s"

private const val FORMAT_FILE_SIZE = "%s: %s"

fun filesToString(context: Context, files: Collection<File>, depth: Int): String {
    val map: MutableMap<File, Long> = LinkedHashMap()
    for (f in files) {
        map[f] = getSize(f, depth)
    }
    return filesWithSizeToString(context, map)
}

fun filePairsToString(context: Context, files: Collection<Pair<File, File>>, depth: Int): String {
    val map = mutableMapOf<Pair<File, File>, Long>()
    for (p in files) {
        if (p.first != null) {
            map[p] = getSize(p.first, depth)
        }
    }
    return filePairsWithSizeToString(context, map)
}

fun filesWithSizeToString(context: Context, files: Map<File, Long>): String {
    return filesWithSizeToString(context, files.entries)
}

/**
 * @param files file < - > size in bytes
 */
fun filesWithSizeToString(context: Context, files: Collection<Map.Entry<File, Long>>): String {
    val result: MutableList<String?> = ArrayList()
    for (f in files) {
        val key = f.key
        val size = f.value
        result.add(String.format(FORMAT_FILE_SIZE, key.absolutePath,
                sizeToString(size.toDouble(), SizeUnit.BYTES, emptySet(), 0) { resId, formatArgs -> context.getString(resId, formatArgs) }))
    }
    return join(NEXT_LINE, result)
}

fun filePairsWithSizeToString(context: Context, files: Map<Pair<File, File>, Long>): String =
        filePairsWithSizeToString(context, files.entries)

/**
 * @param files pair < source file - destination file > <-> size in bytes
 */
fun filePairsWithSizeToString(context: Context, files: Collection<Map.Entry<Pair<File, File>, Long>>): String {
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
            result.add(String.format(FORMAT_FILE_SIZE, sb.toString(),
                    sizeToString(size.toDouble(), SizeUnit.BYTES, emptySet(), 0) { resId, formatArgs ->
                        context.getString(resId, formatArgs)
                    }))
        }
    }
    return join(NEXT_LINE, result)
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
    map.forEach {
        when (it.key) {
            SizeUnit.BYTES -> {
                result.add(String.format(FORMAT_SIZE, it.value, stringsProvider(R.plurals.size_suffix_bytes, it.value)))
            }
            SizeUnit.KBYTES -> {
                result.add(String.format(FORMAT_SIZE, it.value, stringsProvider(R.plurals.size_suffix_kbytes, it.value)))
            }
            SizeUnit.MBYTES -> {
                result.add(String.format(FORMAT_SIZE, it.value, stringsProvider(R.plurals.size_suffix_mbytes, it.value)))
            }
            SizeUnit.GBYTES -> {
                result.add(String.format(FORMAT_SIZE, it.value, stringsProvider(R.plurals.size_suffix_gbytes, it.value)))
            }
            else -> {
                // do noting
            }
        }
    }
    return join(", ", result)
}
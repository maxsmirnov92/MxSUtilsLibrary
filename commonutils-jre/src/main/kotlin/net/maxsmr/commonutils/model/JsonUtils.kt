@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.model

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.text.EMPTY_STRING
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("JsonUtils")

fun JSONObject?.isJsonField(fieldName: String?): Boolean {
    if (this != null) {
        val value = opt(fieldName)
        return value is JSONObject || value is JSONArray
    }
    return false
}

/**
 * @param target имя параметра
 * @return значение параметра или пустая строка при отсутствии значения или при значении "NULL"
 */
@JvmOverloads
fun JSONObject.optStringNonNull(target: String, fallback: String? = EMPTY_STRING): String =
    optString(target, fallback.orEmpty()).takeIf {
        !net.maxsmr.commonutils.text.isEmpty(it, true)
    }.orEmpty()

@JvmOverloads
fun JSONObject.optStringNullable(key: String, fallback: String? = EMPTY_STRING) =
    if (isNull(key)) {
        null
    } else {
        optString(key, fallback.orEmpty()).takeIf { !net.maxsmr.commonutils.text.isEmpty(it, true) }
    }

@JvmOverloads
fun JSONArray.optStringNullable(index: Int, fallback: String? = EMPTY_STRING) =
    if (isNull(index)) {
        null
    } else {
        optString(index, fallback).takeIf { !net.maxsmr.commonutils.text.isEmpty(it, true) }
    }

/**
 * Возвращает null в ситуациях, когда оригинальный метод вернул бы fallback.
 * Возвращает [Integer.MAX_VALUE] или [Integer.MIN_VALUE] когда извлекаемое значение в действительности [Long],
 * который соотв. больше или меньше чем [Integer.MAX_VALUE] или [Integer.MIN_VALUE].
 * В остальном поведение идентично [org.json.JSONObject.optInt].
 */
fun JSONObject.optIntNullable(key: String) = if (isNull(key)) null else toInteger(opt(key))

/**
 * Возвращает null в ситуациях, когда оригинальный метод вернул бы fallback.
 * Возвращает [Integer.MAX_VALUE] или [Integer.MIN_VALUE] когда извлекаемое значение в действительности [Long],
 * который соотв. больше или меньше чем [Integer.MAX_VALUE] или [Integer.MIN_VALUE].
 * В остальном поведение идентично [org.json.JSONObject.optInt].
 */
fun JSONArray.optIntNullable(index: Int) = if (isNull(index)) null else toInteger(opt(index))

/**
 * Возвращает null в ситуациях, когда оригинальный метод вернул бы fallback.
 * В остальном поведение идентично [org.json.JSONObject.optLong].
 */
fun JSONObject.optLongNullable(key: String) = if (isNull(key)) null else toLong(opt(key))

/**
 * Возвращает null в ситуациях, когда оригинальный метод вернул бы fallback.
 * В остальном поведение идентично [org.json.JSONObject.optLong].
 */
fun JSONArray.optLongNullable(index: Int) = if (isNull(index)) null else toLong(opt(index))

/**
 * Возвращает null в ситуациях, когда оригинальный метод вернул бы fallback или [Double.NaN].
 * В остальном поведение идентично [org.json.JSONObject.optDouble].
 */
fun JSONObject.optDoubleNullable(key: String) = if (isNull(key)) null else toDouble(opt(key))

/**
 * Возвращает null в ситуациях, когда оригинальный метод вернул бы fallback или [Double.NaN].
 * В остальном поведение идентично [org.json.JSONObject.optDouble].
 */
fun JSONArray.optDoubleNullable(index: Int) =
    if (isNull(index)) null else toDouble(opt(index))?.takeIf { !it.isNaN() }

private fun toInteger(value: Any?) = when (value) {
    is Int -> value.toInt()
    is Long -> if (value > Int.MAX_VALUE || value < Int.MIN_VALUE) value.toDouble().toInt() else value.toInt()
    is Number -> value.toInt()
    else ->
        try {
            (value as? String)?.toDouble()?.toInt()
        } catch (ignored: NumberFormatException) {
            null
        }
}

private fun toLong(value: Any?) = when (value) {
    is Number -> value.toLong()
    is String ->
        try {
            value.toDouble().toLong()
        } catch (ignored: NumberFormatException) {
            null
        }
    else -> null
}

private fun toDouble(value: Any?) = when (value) {
    is Number -> value.toDouble()
    is String ->
        try {
            value.toDouble()
        } catch (ignored: NumberFormatException) {
            null
        }
    else -> null
}

fun String.toJSONObjectOrNull(): JSONObject? = try {
    JSONObject(this)
} catch (e: JSONException) {
    null
}

@Throws(JSONException::class)
fun JSONObject?.copyFields(target: JSONObject?) {
    if (target != null && this != null) {
        val it = this.keys()
        while (it.hasNext()) {
            val key = it.next()
            target.put(key, this[key])
        }
    }
}

fun List<JSONPresentable>?.toJSONArray() = JSONArray(this?.map(JSONPresentable::asJSON))

fun JSONObject?.optIntArray(arrayKey: String?): IntArray {
    if (this == null || arrayKey == null) return IntArray(0)
    val jArray = optJSONArray(arrayKey) ?: return IntArray(0)
    return try {
        IntArray(jArray.length()) { jArray.getInt(it) }
    } catch (e: JSONException) {
        logger.e(formatException(e))
        IntArray(0)
    }
}

fun JSONObject?.optLongArray(arrayKey: String?): LongArray {
    if (this == null || arrayKey == null) return LongArray(0)
    val jArray = optJSONArray(arrayKey) ?: return LongArray(0)
    return try {
        LongArray(jArray.length()) { jArray.getInt(it).toLong() }
    } catch (e: JSONException) {
        logger.e(formatException(e))
        LongArray(0)
    }
}

fun JSONObject?.optStringArray(arrayKey: String?): Array<String> {
    if (this == null || arrayKey == null) return emptyArray()
    val jArray = optJSONArray(arrayKey) ?: return emptyArray()
    return try {
        Array(jArray.length()) { jArray.getString(it) }
    } catch (e: JSONException) {
        logger.e(formatException(e))
        emptyArray()
    }
}

/**
 * Находит в текущем JSONObject массив с ключом [arrayKey], и парсит его,
 *
 * @return Список объектов [T] или пустой список. Игнорирует нулевые элементы.
 */
fun <T> JSONObject?.optList(arrayKey: String, parcel: JSONParcel<T>): List<T> =
    this?.optJSONArray(arrayKey)?.let { jsonArray ->
        jsonArray.optList(parcel)
    }.orEmpty()

/**
 * @return массив объектов T из JSONParcel, исходя из того, что в каждом индексе исходного array лежит JSONObject
 */
fun <T> JSONArray?.optList(parcel: JSONParcel<T>): MutableList<T> {
    val result = mutableListOf<T>()
    if (this != null) {
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            val item: T = parcel.fromJSONObject(o) ?: continue
            result.add(item)
        }
    }
    return result
}

/**
 * Список [String] или пустой список
 */
fun JSONObject?.optStringList(arrayKey: String) = optListIndexed(arrayKey) { jsonArray, index ->
    jsonArray.optStringNullable(index)
}

/**
 * Список [Int] или пустой список
 */
fun JSONObject?.optIntList(arrayKey: String) = optListIndexed(arrayKey) { jsonArray, index ->
    jsonArray.optIntNullable(index)
}

/**
 * Список [Long] или пустой список
 */
fun JSONObject?.optLongList(arrayKey: String) = optListIndexed(arrayKey) { jsonArray, index ->
    jsonArray.optLongNullable(index)
}

/**
 * Список [Double] или пустой список
 */
fun JSONObject?.optDoubleList(arrayKey: String) = optListIndexed(arrayKey) { jsonArray, index ->
    jsonArray.optDoubleNullable(index)
}

fun JSONObject?.optBooleanList(arrayKey: String) = optListIndexed(arrayKey) { jsonArray, index ->
    jsonArray.optBoolean(index)
}

/**
 * Находит в текущем JSONObject массив с ключом [arrayKey], и парсит его.
 * @return Список объектов [T] или пустой список. Не добавляет нулевые элементы.
 */
fun <T> JSONObject?.optListIndexed(
    arrayKey: String,
    parcel: JSONParcelArrayIndexed<T>,
) = this?.optJSONArray(arrayKey)?.let { jsonArray ->
    val result = mutableListOf<T>()
    for (i in 0 until jsonArray.length()) {
        parcel.fromJSONArray(jsonArray, i)?.let(result::add)
    }
    return@let result
}.orEmpty()


/**
 * Интерфейс для классов с целью форматирования в [JSONObject]
 */
fun interface JSONPresentable {

    @Throws(JSONException::class)
    fun asJSON(): JSONObject?
}

fun interface JSONParcel<T> {

    /**
     * обработка Exception не предполагается: не бросать, get не вызывать
     */
    fun fromJSONObject(o: JSONObject): T
}

fun interface JSONParcelArray<T> {

    fun fromJSONArray(jsonArray: JSONArray): T
}

fun interface JSONParcelArrayIndexed<T> {

    fun fromJSONArray(jsonArray: JSONArray, index: Int): T
}
@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.model

import net.maxsmr.commonutils.text.EMPTY_STRING
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

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

@Throws(JSONException::class)
fun List<JSONPresentable?>.asJSONArray(): JSONArray {
    val result = JSONArray()
    for (p in this) {
        p?.let {
            result.put(p.asJSON())
        }
    }
    return result
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

fun String.asJSONObjectOrNull(): JSONObject? = try {
    JSONObject(this)
} catch (e: JSONException) {
    null
}

fun Collection<Any>.asJSONArray(): JSONArray = JSONArray().also {
    val iterator = iterator()
    while (iterator.hasNext()) {
        it.put(iterator.next())
    }
}

fun IntArray.asJSONArray(): JSONArray = JSONArray().also {
    val iterator = iterator()
    while (iterator.hasNext()) {
        it.put(iterator.next())
    }
}

fun LongArray.asJSONArray(): JSONArray = JSONArray().also {
    val iterator = iterator()
    while (iterator.hasNext()) {
        it.put(iterator.next())
    }
}

/**
 * Находит в текущем JSONObject массив с ключом [key], и парсит его,
 * применяя fromJSONObject из [JSONParcel] к каждому элементу
 */
fun <T> JSONObject?.asList(key: String, parcel: JSONParcel<T?>): List<T> {
    return this?.optJSONArray(key)?.let {
        it.asList(parcel)
    }.orEmpty()
}

/**
 * @return массив объектов T из JSONParcel, исходя из того, что в каждом индексе исходного array лежит JSONObject
 */
fun <T> JSONArray?.asList(parcel: JSONParcel<T?>): MutableList<T> {
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
 * Находит в текущем JSONObject массив с ключом [key], и парсит его,
 * применяя fromJSONArray из [JSONParcelArrayIndexed] к каждому элементу
 */
fun <T> JSONObject?.asListIndexed(key: String, parcel: JSONParcelArrayIndexed<T?>): List<T> {
    return this?.optJSONArray(key)?.let {
        it.asListIndexed(parcel)
    }.orEmpty()
}

@Suppress("UNCHECKED_CAST")
fun <T> JSONArray?.asPrimitiveList(clazz: Class<T>): List<T> {
    return asListIndexed { jsonArray: JSONArray, index: Int ->
        when {
            clazz.isAssignableFrom(String::class.java) -> {
                return@asListIndexed jsonArray.optString(index) as T
            }
            clazz.isAssignableFrom(Int::class.java) -> {
                return@asListIndexed jsonArray.optInt(index) as T
            }
            clazz.isAssignableFrom(Long::class.java) -> {
                return@asListIndexed jsonArray.optLong(index) as T
            }
            clazz.isAssignableFrom(Double::class.java) -> {
                return@asListIndexed jsonArray.optDouble(index) as T
            }
            clazz.isAssignableFrom(Boolean::class.java) -> {
                return@asListIndexed jsonArray.optBoolean(index) as T
            }
            else -> {
                throw RuntimeException("Incorrect primitive class: $clazz")
            }
        }
    }
}

/**
 * @return массив объектов T из JSONParcel, отдавая каждый индекс исходного array в parcel на его усмотрение
 */
fun <T> JSONArray?.asListIndexed(parcel: JSONParcelArrayIndexed<T?>): MutableList<T> {
    val result = mutableListOf<T>()
    if (this != null) {
        for (i in 0 until length()) {
            val item: T = parcel.fromJSONArray(this, i) ?: continue
            result.add(item)
        }
    }
    return result
}

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
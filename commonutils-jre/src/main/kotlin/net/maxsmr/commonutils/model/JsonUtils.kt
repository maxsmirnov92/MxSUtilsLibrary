@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.model

import com.google.gson.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface JSONPresentable {
    @Throws(JSONException::class)
    fun asJSON(): JSONObject?
}

interface JSONParcel<T> {
    fun fromJSONObject(o: JSONObject?): T?
}


interface JSONParcelArrayIndexed<T> {
    fun fromJSONArray(jsonArray: JSONArray, index: Int): T
}

@Throws(JSONException::class)
fun asJSONArray(list: List<JSONPresentable?>?): JSONArray {
    val result = JSONArray()
    if (list != null) {
        for (p in list) {
            p?.let {
                result.put(p.asJSON())
            }
        }
    }
    return result
}

fun <T> asList(array: JSONArray?, parcel: JSONParcel<T>): List<T?> = asList(array, parcel, false)

fun <T> asListNonNull(array: JSONArray?, parcel: JSONParcel<T>): List<T> = asList(array, parcel, true) as List<T>

fun <T> asListIndexed(array: JSONArray?, parcel: JSONParcelArrayIndexed<T>): List<T?> = asListIndexed(array, parcel, false)

fun <T> asListIndexedNonNull(array: JSONArray?, parcel: JSONParcelArrayIndexed<T>): List<T> = asListIndexed(array, parcel, true) as List<T>

fun isJsonFieldJson(jsonObject: JSONObject?, fieldName: String?): Boolean {
    if (jsonObject != null) {
        val value = jsonObject.opt(fieldName)
        return value is JSONObject || value is JSONArray
    }
    return false
}

@Throws(JSONException::class)
fun copyFields(source: JSONObject?, target: JSONObject?) {
    if (target != null && source != null) {
        val it = source.keys()
        while (it.hasNext()) {
            val key = it.next()
            target.put(key, source[key])
        }
    }
}

/**
 * @return массив объектов T из JSONParcel, исходя из того, что в каждом индексе исходного array лежит JSONObject
 */
private fun <T> asList(array: JSONArray?, parcel: JSONParcel<T>, nonNull: Boolean): List<T?> {
    val result = mutableListOf<T?>()
    if (array != null) {
        for (i in 0 until array.length()) {
            val o: JSONObject? = array.optJSONObject(i)
            val item: T? = parcel.fromJSONObject(o)
            if (!nonNull || item != null) {
                result.add(item)
            }
        }
    }
    return result
}

/**
 * @return массив объектов T из JSONParcel, отдавая каждый индекс исходного array в parcel на его усмотрение
 */
private fun <T> asListIndexed(array: JSONArray?, parcel: JSONParcelArrayIndexed<T>, nonNull: Boolean): List<T?> {
    val result = mutableListOf<T?>()
    if (array != null) {
        for (i in 0 until array.length()) {
            val item: T? = parcel.fromJSONArray(array, i)
            if (!nonNull || item != null) {
                result.add(item)
            }
        }
    }
    return result
}

fun <T> asPrimitiveList(array: JSONArray?, clazz: Class<T>, nonNull: Boolean): List<T?> = asListIndexed(array, object : JSONParcelArrayIndexed<T> {

    override fun fromJSONArray(jsonArray: JSONArray, index: Int): T {
        return when {
            clazz.isAssignableFrom(String::class.java) -> {
                jsonArray.optString(index) as T
            }
            clazz.isAssignableFrom(Int::class.java) -> {
                jsonArray.optInt(index) as T
            }
            clazz.isAssignableFrom(Long::class.java) -> {
                jsonArray.optLong(index) as T
            }
            clazz.isAssignableFrom(Double::class.java) -> {
                jsonArray.optDouble(index) as T
            }
            clazz.isAssignableFrom(Boolean::class.java) -> {
                jsonArray.optBoolean(index) as T
            }
            else -> {
                throw RuntimeException("Incorrect primitive class: $clazz")
            }
        }
    }
}, nonNull)
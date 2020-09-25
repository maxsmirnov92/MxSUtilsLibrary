@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.data.gson

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
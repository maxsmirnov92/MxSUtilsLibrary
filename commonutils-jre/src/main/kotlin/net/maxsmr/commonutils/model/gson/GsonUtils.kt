@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.model.gson

import com.google.gson.*
import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.reflect.TypeToken
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.text.isEmpty
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.*

//Вспомогательные утилиты для работы с json
//посредством [Gson]

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("GsonUtils")

/**
 * Преобразует строку [jsonString] в инстанс указанного типа [T],
 * информация о котором содержится в [classOfT],
 * используя [gson]
 */
fun <T> fromJsonObjectString(gson: Gson, jsonString: String?, classOfT: Class<T>): T? =
        fromJsonObjectString<T>(gson, jsonString, classOfT as Type)

/**
 * Преобразует строку [jsonString] в инстанс указанного типа [T],
 * информация о котором содержится в [typeToken],
 * используя [gson]
 */
fun <T> fromJsonObjectString(gson: Gson, jsonString: String?, typeToken: TypeToken<T>): T? =
        fromJsonObjectString<T>(gson, jsonString, typeToken.type)

/**
 * Преобразует строку [jsonString] в список сущностей указанного типа [T],
 * информация о котором содержится в [type],
 * используя [gson]
 */
fun <T> fromJsonArrayString(gson: Gson, jsonString: String?, type: Class<Array<T>?>): List<T> {
    val array: Array<T>? = fromJsonObjectString(gson, jsonString, type)
    return if (array != null) {
        listOf(*array)
    } else {
        emptyList()
    }
}

/**
 * Преобразует строку [jsonString] в инстанс указанного типа [T],
 * информация о котором содержится в [type],
 * используя [gson]
 */
fun <T> fromJsonObjectString(gson: Gson, jsonString: String?, type: Type): T? {
    if (!isEmpty(jsonString)) {
        try {
            return gson.fromJson(jsonString, type)
        } catch (e: JsonParseException) {
            logException(logger, e, "fromJson")
        }
    }
    return null
}

/**
 * Преобразует объект [obj] указанного типа [T]
 * в строку, используя [gson]
 */
@JvmOverloads
fun <T : Any> toJsonString(
        gson: Gson,
        obj: T,
        type: Type = obj.javaClass
): String {
    try {
        return gson.toJson(obj, type)
    } catch (e: JsonParseException) {
        logException(logger, e, "toJson")
    }
    return EMPTY_STRING
}

/**
 * Преобразует коллекцию объектов [listOfObjects] указанного типа [T]
 * в маппинг: объект - json-строка, используя [gson]
 */
fun <T : Any> toJsonStringMap(
        gson: Gson,
        listOfObjects: Collection<T>?,
        type: Type
): Map<T, String> {
    val result: MutableMap<T, String> = LinkedHashMap()
    if (listOfObjects != null) {
        for (o in listOfObjects) {
            result[o] = toJsonString(gson, o, type)
        }
    }
    return result
}

fun isJsonFieldNull(jsonObject: JsonObject, memberName: String?): Boolean {
    val element = getJsonElementAs(jsonObject[memberName], JsonElement::class.java)
    return element == null || element.isJsonNull
}

fun <J : JsonElement?> toJsonElement(string: String?, clazz: Class<J>
): J? {
    var element: JsonElement? = null
    if (!isEmpty(string)) {
        try {
            element = JsonParser.parseString(string)
        } catch (e: JsonParseException) {
            logException(logger, e, "parse")
        }
    }
    return if (element != null && clazz.isInstance(element)) {
        element as J
    } else {
        null
    }
}

fun <P : Number?> getPrimitiveNumber(obj: Any?, clazz: Class<P>): P? {
    return if (obj != null && clazz.isInstance(obj)) obj as P else null
}

fun <P : Number?> getPrimitiveNumber(element: JsonElement?): P? {
    if (element !is JsonPrimitive) {
        return null
    }
    return if (element.isNumber) element.asNumber as P else null
}

fun getPrimitiveString(obj: Any?): String? {
    return if (obj is String) obj else null
}

fun getPrimitiveString(element: JsonElement?): String? {
    if (element !is JsonPrimitive) {
        return null
    }
    return if (element.isString) element.asString else null
}

fun getPrimitiveBoolean(obj: Any?): Boolean {
    return if (obj is Boolean) obj else false
}

fun getPrimitiveBoolean(element: JsonElement?): Boolean? {
    if (element !is JsonPrimitive) {
        return null
    }
    return if (element.isBoolean) element.asBoolean else null
}

fun <E : JsonElement> getJsonElementAs(jsonElement: JsonElement?, clazz: Class<E>): E? {
    var result: E? = null
    if (jsonElement != null) {
        when {
            jsonElement is JsonNull && clazz == JsonNull::class.java ->
                result = jsonElement as E
            jsonElement is JsonPrimitive && clazz == JsonPrimitive::class.java ->
                result = jsonElement as E
            jsonElement is JsonObject && clazz == JsonObject::class.java ->
                result = jsonElement as E
            jsonElement is JsonArray && clazz == JsonArray::class.java ->
                result = jsonElement as E
        }
    }
    return result
}

@JvmOverloads
fun <V> getJsonPrimitiveAs(forElement: JsonPrimitive?, clazz: Class<V>, defaultValue: V? = null): V? {
    var value: V? = null
    if (forElement != null) {
        if (forElement.isString && clazz == String::class.java) {
            value = forElement.asString as V
        } else if (forElement.isNumber && Number::class.java.isAssignableFrom(clazz)) {
            val numberValue = forElement.asNumber
            // любой наследник от Number реализует эти методы
            value = when (clazz) {
                Int::class.java -> numberValue.toInt() as V
                Long::class.java -> numberValue.toLong() as V
                Short::class.java -> numberValue.toShort() as V
                Double::class.java -> numberValue.toDouble() as V
                Float::class.java -> numberValue.toFloat() as V
                Byte::class.java -> numberValue.toByte() as V
                Char::class.java -> numberValue.toChar() as V
                else -> null
            }
        } else if (forElement.isBoolean && clazz == Boolean::class.java) {
            value = forElement.asBoolean as V
        }
    }
    return value ?: defaultValue
}

fun <E : JsonElement> getJsonElement(jsonElement: JsonElement?, memberName: String?, clazz: Class<E>): E? {
    return getJsonElementAs(jsonElement, JsonObject::class.java)?.let {
        getJsonElementAs(it[memberName], clazz)
    }
}

@JvmOverloads
fun <V> getJsonPrimitive(jsonElement: JsonElement?, memberName: String?, clazz: Class<V>, defaultValue: V? = null): V? {
    return getJsonPrimitiveAs(getJsonElement(jsonElement, memberName, JsonPrimitive::class.java), clazz, defaultValue)
}

@JvmOverloads
fun <V> getJsonPrimitive(jsonElement: JsonElement?, clazz: Class<V>, defaultValue: V? = null): V? {
    return getJsonPrimitiveAs(getJsonElementAs(jsonElement, JsonPrimitive::class.java), clazz, defaultValue)
}

@JvmOverloads
fun <V> getFromJsonArray(array: JsonArray?, parcel: JsonParcelArray<V>, nonNull: Boolean = false): List<V?> {
    val result = mutableListOf<V?>()
    array?.let {
        for (i in 0 until array.size()) {
            val item = parcel.fromJsonArray(array, i)
            if (!nonNull || item != null) {
                result.add(item)
            }
        }
    }
    return result
}

fun Any?.toJsonElement(): JsonElement {
    var value: JsonElement = JsonNull.INSTANCE
    when (this) {
        is JSONObject -> {
            value = JsonObject().apply {
                this@toJsonElement.keys().forEach { key ->
                    add(key, this.get(key).toJsonElement())
                }
            }
        }
        is JSONArray -> {
            value = JsonArray().apply {
                for (i in 0 until this@toJsonElement.length()) {
                    add(this.get(i).toJsonElement())
                }
            }
        }
        is Boolean -> {
            value = JsonPrimitive(this)
        }
        is Number -> {
            value = JsonPrimitive(this)
        }
        is String -> {
            value = JsonPrimitive(this)
        }
        is Char -> {
            value = JsonPrimitive(this)
        }
    }
    return value
}

fun JsonObject.mergeJsonObject(fromAnother: JsonObject) {
    fromAnother.keySet().forEach {
        add(it, fromAnother[it])
    }
}

fun JsonArray.mergeJsonArray(fromAnother: JsonArray) {
    for (i in 0 until fromAnother.size()) {
        add(fromAnother.get(i))
    }
}

interface JsonParcelArray<T> {

    fun fromJsonArray(jsonArray: JsonArray?, index: Int): T?
}

abstract class JsonParcelArrayObjects<T>: JsonParcelArray<T> {

    abstract fun fromJsonObject(jsonObj: JsonObject?): T?

    override fun fromJsonArray(jsonArray: JsonArray?, index: Int): T? {
        return jsonArray?.let { fromJsonObject(getJsonElementAs(jsonArray.get(index), JsonObject::class.java)) }
    }
}
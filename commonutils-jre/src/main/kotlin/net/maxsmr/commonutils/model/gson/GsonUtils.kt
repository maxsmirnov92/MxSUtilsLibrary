@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.model.gson

import com.google.gson.*
import com.google.gson.JsonParser.parseString
import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
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
        logException(logger, e, "toJson", false)
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

fun <J : JsonElement?> toJsonElement(
    string: String?,
    clazz: Class<J>
): J? = try {
    toJsonElementOrThrow(string, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "toJsonElement", false)
    null
}

@Throws(JsonParseException::class)
fun <J : JsonElement?> toJsonElementOrThrow(
    string: String?,
    clazz: Class<J>
): J {
    var element: JsonElement? = null
    if (string != null && string.isNotEmpty()) {
        try {
            element = parseString(string)
        } catch (e: JsonParseException) {
            throw e
        }
    }
    return if (element != null && clazz.isInstance(element)) {
        element as J
    } else {
        throw JsonParseException("Json element $element is not instance of $clazz")
    }
}

fun <J : JsonElement?> toJsonElement(
    parser: JsonReader,
    clazz: Class<J>,
): J? = try {
    toJsonElementOrThrow(parser, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "toJsonElement", false)
    null
}

@Throws(JsonParseException::class)
fun <J : JsonElement?> toJsonElementOrThrow(parser: JsonReader, clazz: Class<J>): J {
    val element: JsonElement? = try {
        Streams.parse(parser)
    } catch (e: JsonParseException) {
        throw e
    }
    return if (element != null && clazz.isInstance(element)) {
        element as J
    } else {
        throw JsonParseException("Json element $element is not instance of $clazz")
    }
}

fun <P : Number?> getPrimitiveNumber(element: JsonElement?): P?  = try {
    getPrimitiveNumberOrThrow(element)
} catch (e: JsonParseException) {
    logException(logger, e, "getPrimitiveNumber", false)
    null
}

@Throws(JsonParseException::class)
fun <P : Number?> getPrimitiveNumberOrThrow(element: JsonElement?): P {
    if (element !is JsonPrimitive) {
        throw JsonParseException("Json element $element is not JsonPrimitive")
    }
    return if (element.isNumber) {
        element.asNumber as P
    } else {
        throw JsonParseException("Json element $element is not Number")
    }
}

fun getPrimitiveString(element: JsonElement?): String? = try {
    getPrimitiveStringOrThrow(element)
} catch (e: JsonParseException) {
    logException(logger, e, "getPrimitiveString", false)
    null
}

@Throws(JsonParseException::class)
fun getPrimitiveStringOrThrow(element: JsonElement?): String {
    if (element !is JsonPrimitive) {
        throw JsonParseException("Json element $element is not JsonPrimitive")
    }
    return if (element.isString) {
        element.asString
    } else {
        throw JsonParseException("Json element $element is not String")
    }
}

fun getPrimitiveBoolean(element: JsonElement?): Boolean? = try {
    getPrimitiveBooleanOrThrow(element)
} catch (e: JsonParseException) {
    logException(logger, e, "getPrimitiveBoolean", false)
    null
}

@Throws(JsonParseException::class)
fun getPrimitiveBooleanOrThrow(element: JsonElement?): Boolean {
    if (element !is JsonPrimitive) {
        throw JsonParseException("Json element $element is not JsonPrimitive")
    }
    return if (element.isBoolean) {
        element.asBoolean
    } else {
        throw JsonParseException("Json element $element is not Boolean")
    }
}

fun <E : JsonElement> getJsonElementAs(jsonElement: JsonElement?, clazz: Class<E>): E? = try {
    getJsonElementAsOrThrow(jsonElement, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonElementAs", false)
    null
}

@Throws(JsonParseException::class)
fun <E : JsonElement> getJsonElementAsOrThrow(element: JsonElement?, clazz: Class<E>): E {
    return when {
        element is JsonNull && JsonNull::class.java.isAssignableFrom(clazz) ->
            element as E
        element is JsonPrimitive && JsonPrimitive::class.java.isAssignableFrom(clazz) ->
            element as E
        element is JsonObject && JsonObject::class.java.isAssignableFrom(clazz) ->
            element as E
        element is JsonArray && JsonArray::class.java.isAssignableFrom(clazz) ->
            element as E
        else -> throw JsonParseException("Incorrect type of json element $element")
    }
}

fun <V> getJsonPrimitiveAsNonNull(forElement: JsonElement?, clazz: Class<V>, defaultValue: V): V =
    getJsonPrimitiveAs(forElement, clazz, defaultValue) ?: defaultValue

@JvmOverloads
fun <V> getJsonPrimitiveAs(element: JsonElement?, clazz: Class<V>, defaultValue: V? = null): V? = try {
    getJsonPrimitiveAsOrThrow(element, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonPrimitiveAs", false)
    defaultValue
}

@Throws(JsonParseException::class)
fun <V> getJsonPrimitiveAsOrThrow(element: JsonElement?, clazz: Class<V>): V =
    getJsonPrimitiveAsOrThrow(getJsonElementAsOrThrow(element, JsonPrimitive::class.java), clazz)

fun <V> getJsonPrimitiveAsNonNull(forElement: JsonPrimitive?, clazz: Class<V>, defaultValue: V): V =
    getJsonPrimitiveAs(forElement, clazz, defaultValue) ?: defaultValue

@JvmOverloads
fun <V> getJsonPrimitiveAs(element: JsonPrimitive?, clazz: Class<V>, defaultValue: V? = null): V? = try {
    getJsonPrimitiveAsOrThrow(element, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonPrimitiveAs", false)
    defaultValue
}

@Throws(JsonParseException::class)
fun <V> getJsonPrimitiveAsOrThrow(element: JsonPrimitive?, clazz: Class<V>): V {
    return if (element != null) {
        if (element.isString && String::class.java.isAssignableFrom(clazz)) {
            element.asString as V
        } else if (element.isNumber) {
            val numberValue = element.asNumber
            when {
                numberValue is LazilyParsedNumber -> {
                    when {
                        Int::class.java.isAssignableFrom(clazz) -> numberValue.toInt() as V
                        Long::class.java.isAssignableFrom(clazz) -> numberValue.toLong() as V
                        Short::class.java.isAssignableFrom(clazz) -> numberValue.toShort() as V
                        Double::class.java.isAssignableFrom(clazz) -> numberValue.toDouble() as V
                        Float::class.java.isAssignableFrom(clazz) -> numberValue.toFloat() as V
                        Byte::class.java.isAssignableFrom(clazz) -> numberValue.toByte() as V
                        Char::class.java.isAssignableFrom(clazz) -> numberValue.toChar() as V
                        else -> throw JsonParseException("Json element $element is not known primitive")
                    }
                }
                Number::class.java.isAssignableFrom(clazz) -> {
                    numberValue as V
                }
                else -> {
                    throw JsonParseException("Json element $element is not Number")
                }
            }
        } else if (element.isBoolean && Boolean::class.java.isAssignableFrom(clazz)) {
            element.asBoolean as V
        } else {
            throw JsonParseException("Json element $element is not Boolean")
        }
    } else {
        throw JsonParseException("Json element $element is null")
    }
}

fun <E : JsonElement> getJsonElement(jsonElement: JsonElement?, memberName: String?, clazz: Class<E>): E? = try {
    getJsonElementOrThrow(jsonElement, memberName, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonElement", false)
    null
}

@Throws(JsonParseException::class)
fun <E : JsonElement> getJsonElementOrThrow(jsonElement: JsonElement?, memberName: String?, clazz: Class<E>): E {
    return getJsonElementAsOrThrow(getJsonElementAsOrThrow(jsonElement, JsonObject::class.java)[memberName], clazz)
}

fun <V> getJsonPrimitiveNonNull(jsonElement: JsonElement?, memberName: String?, clazz: Class<V>, defaultValue: V): V =
    getJsonPrimitive(jsonElement, memberName, clazz) ?: defaultValue

fun <V> getJsonPrimitive(jsonElement: JsonElement?, memberName: String?, clazz: Class<V>): V? = try {
    getJsonPrimitiveOrThrow(jsonElement, memberName, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonPrimitive", false)
    null
}

@Throws(JsonParseException::class)
fun <V> getJsonPrimitiveOrThrow(jsonElement: JsonElement?, memberName: String?, clazz: Class<V>): V {
    return getJsonPrimitiveAsOrThrow(getJsonElementOrThrow(jsonElement, memberName, JsonPrimitive::class.java), clazz)
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

    abstract fun fromJsonObject(jsonObj: JsonObject): T?

    override fun fromJsonArray(jsonArray: JsonArray?, index: Int): T? {
        return jsonArray?.let { fromJsonObject(getJsonElementAsOrThrow(jsonArray.get(index), JsonObject::class.java)) }
    }
}
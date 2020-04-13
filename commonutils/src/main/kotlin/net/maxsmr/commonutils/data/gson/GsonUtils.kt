@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.data.gson

import android.text.TextUtils
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.isEmpty
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.lang.reflect.Type
import java.util.*

//Вспомогательные утилиты для работы с json
//посредством [Gson]

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("GsonUtils")

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
        ArrayList(listOf(*array))
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
    if (!TextUtils.isEmpty(jsonString)) {
        try {
            return gson.fromJson(jsonString, type)
        } catch (e: JsonParseException) {
            logger.e("A JsonParseException occurred during fromJson(): " + e.message, e)
        }
    }
    return null
}

/**
 * Преобразует объект [obj] указанного типа [T]
 * в строку, используя [gson]
 */
fun <T> toJsonString(gson: Gson, obj: T): String {
    try {
        return gson.toJson(obj)
    } catch (e: JsonParseException) {
        logger.e("an JsonParseException occurred during toJson(): " + e.message, e)
    }
    return EMPTY_STRING
}

/**
 * Преобразует коллекцию объектов [listOfObjects] указанного типа [T]
 * в маппинг: объект - json-строка, используя [gson]
 */
fun <T> toJsonStringMap(gson: Gson, listOfObjects: Collection<T>?): Map<T, String> {
    val result: MutableMap<T, String> = LinkedHashMap()
    if (listOfObjects != null) {
        for (o in listOfObjects) {
            result[o] = toJsonString(gson, o)
        }
    }
    return result
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
            jsonElement is JsonNull && clazz.isAssignableFrom(JsonNull::class.java) ->
                result = jsonElement as E
            jsonElement is JsonPrimitive && clazz.isAssignableFrom(JsonPrimitive::class.java) ->
                result = jsonElement as E
            jsonElement is JsonObject && clazz.isAssignableFrom(JsonObject::class.java) ->
                result = jsonElement as E
            jsonElement is JsonArray && clazz.isAssignableFrom(JsonArray::class.java) ->
                result = jsonElement as E
        }
    }
    return result
}

fun <E : JsonElement> getJsonElement(jsonElement: JsonElement?, memberName: String?, clazz: Class<E>): E? {
    var value: E? = null
    if (jsonElement != null) {
        if (jsonElement.isJsonObject) {
            val obj = jsonElement.asJsonObject
            value = getJsonElementAs(obj[memberName], clazz)
        }
    }
    return value
}

@JvmOverloads
fun <V> getJsonPrimitiveAs(forElement: JsonPrimitive?, clazz: Class<V>, defaultValue: V? = null): V? {
    var value: V? = null
    if (forElement != null) {
        if (forElement.isString && clazz.isAssignableFrom(String::class.java)) {
            value = forElement.asString as V
        } else if (forElement.isNumber && clazz.isAssignableFrom(Number::class.java)) {
            value = forElement.asNumber as V
        } else if (forElement.isBoolean && clazz.isAssignableFrom(Boolean::class.java)) {
            value = forElement.asBoolean as V
        }
    }
    return value ?: defaultValue
}

@JvmOverloads
fun <V> getJsonPrimitive(jsonElement: JsonElement?, memberName: String?, clazz: Class<V>, defaultValue: V? = null): V? {
    return getJsonPrimitiveAs(getJsonElement(jsonElement, memberName, JsonPrimitive::class.java), clazz, defaultValue)
}

fun <J : JsonElement?> asJsonElement(
        parser: JsonParser = JsonParser(),
        string: String?,
        clazz: Class<J>
): J? {
    var element: JsonElement? = null
    if (!isEmpty(string)) {
        try {
            element = parser.parse(string)
        } catch (e: JsonParseException) {
            logger.e("an JsonParseException occurred during parse(): " + e.message, e)
        }
    }
    return if (element != null && clazz.isInstance(element)) {
        element as J
    } else {
        null
    }
}
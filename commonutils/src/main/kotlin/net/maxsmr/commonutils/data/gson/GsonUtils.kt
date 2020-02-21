@file:Suppress("UNCHECKED_CAST")

package net.maxsmr.commonutils.data.gson

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.maxsmr.commonutils.data.EMPTY_STRING
import net.maxsmr.commonutils.data.StringUtils
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
fun <T> fromJsonObjectString(gson: Gson, jsonString: String?, classOfT: Class<T>): T? {
    return fromJsonObjectString(gson, jsonString, classOfT as Type)
}

/**
 * Преобразует строку [jsonString] в инстанс указанного типа [T],
 * информация о котором содержится в [typeToken],
 * используя [gson]
 */
fun <T> fromJsonObjectString(gson: Gson, jsonString: String?, typeToken: TypeToken<T>): T? {
    return fromJsonObjectString(gson, jsonString, typeToken.type)
}

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
    try {
        return gson.fromJson(jsonString, type)
    } catch (e: JsonParseException) {
        logger.e("an JsonParseException occurred during fromJson(): " + e.message, e)
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

fun <P : Number?> getPrimitiveNumber(element: JsonElement?, clazz: Class<P>): P? {
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

fun <V> getJsonPrimitiveValueIn(inElement: JsonElement?, memberName: String?, clazz: Class<V>): V? {
    var value: V? = null
    if (inElement != null) {
        if (inElement.isJsonObject) {
            val obj = inElement.asJsonObject
            value = getJsonPrimitiveValueFor(obj[memberName], clazz)
        }
    }
    return value
}

fun <V> getJsonPrimitiveValueFor(forElement: JsonElement?, clazz: Class<V>): V? {
    var value: V? = null
    if (forElement != null && forElement.isJsonPrimitive) {
        val primitive = forElement.asJsonPrimitive
        if (primitive.isString && clazz.isAssignableFrom(String::class.java)) {
            value = primitive.asString as V
        } else if (primitive.isNumber && clazz.isAssignableFrom(Number::class.java)) {
            value = primitive.asNumber as V
        } else if (primitive.isBoolean && clazz.isAssignableFrom(Boolean::class.java)) {
            value = java.lang.Boolean.valueOf(primitive.asBoolean) as V
        }
    }
    return value
}

fun <J : JsonElement?> asJsonElement(
        parser: JsonParser = JsonParser(),
        string: String?,
        clazz: Class<J>
): J? {
    var element: JsonElement? = null
    if (!StringUtils.isEmpty(string)) {
        try {
            element = parser.parse(string)
        } catch (e: JsonParseException) {
            logger.e("an JsonParseException occurred during parse(): " + e.message, e)
        }
    }
    return if (element != null && clazz.isInstance(element)) {
        element as J
    } else null
}
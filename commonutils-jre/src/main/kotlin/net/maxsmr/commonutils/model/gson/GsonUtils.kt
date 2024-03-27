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
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type
import java.util.*

// Вспомогательные утилиты для работы с json
// посредством [Gson]

private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("GsonUtils")

/**
 * Преобразует строку [jsonString] в инстанс указанного **generic** типа [T],
 * информация о котором содержится в [classOfT],
 * используя [this]
 */
fun <T> Gson.fromJsonOrNull(jsonString: String?, classOfT: Class<T>): T? =
    fromJsonOrNull<T>(jsonString, classOfT as Type)

/**
 * Преобразует строку [jsonString] в инстанс указанного **generic** типа [T],
 * информация о котором содержится в [typeToken],
 * используя [this]
 */
fun <T> Gson.fromJsonOrNull(jsonString: String?, typeToken: TypeToken<T>): T? =
    fromJsonOrNull(jsonString, typeToken.type)

/**
 * Использовать, если [T] **сам** является generic типом.
 *
 * Аналог [Gson.fromJson] (перегрузка с [Type] параметром), возвращающий null в случае возникновения исключения.
 */
fun <T> Gson.fromJsonOrNull(jsonString: String?, type: Type): T? {
    return try {
        fromJson(jsonString, type)
    } catch (e: JsonParseException) {
        logException(logger, e, "fromJsonOrNull")
        null
    }
}

/**
 * Преобразует строку [jsonString] в список сущностей указанного типа [T],
 * информация о котором содержится в [type],
 */
fun <T> Gson.fromJsonArrayOrNull(jsonString: String?, type: Class<Array<T>?>): List<T> {
    val array: Array<T>? = fromJsonOrNull(jsonString, type)
    return if (array != null) {
        listOf(*array)
    } else {
        emptyList()
    }
}

/**
 * Использовать, если [T] **сам** является generic типом.
 *
` * Аналог [Gson.toJson] (перегрузка с [Type] параметром),
 * возвращающий null в случае возникновения исключения.
 */
fun <T : Any> Gson.toJsonOrNull(obj: T, type: Type): String? {
    return try {
        return toJson(obj, type)
    } catch (e: JsonParseException) {
        logException(logger, e, "toJsonOrNull")
        null
    }
}

/**
 * Использовать, если [T] **сам не** является generic типом (но может иметь generic поля).
 *
 * Аналог [Gson.toJson] со следующими отличиями:
 * - возвращает null в случае возникновения любого исключения
 * - возвращает null в случае, если [obj] == null (в дефолтном методе возвращается [JsonNull])
 */
fun <T : Any> Gson.toJsonOrNull(obj: T): String? {
    return try {
        return toJson(obj)
    } catch (e: JsonParseException) {
        logException(logger, e, "toJsonOrNull")
        null
    }
}

/**
 * Преобразует коллекцию объектов [listOfObjects] указанного типа [T]
 * в маппинг: объект - json-строка, используя [gson]
 */
fun <T : Any> Gson.toJsonStringMap(
    listOfObjects: Collection<T>?,
    type: Type
): Map<T, String?> {
    val result: MutableMap<T, String?> = LinkedHashMap()
    if (listOfObjects != null) {
        for (o in listOfObjects) {
            result[o] = toJsonOrNull(o, type)
        }
    }
    return result
}

val JsonElement?.asLongOrNull: Long?
    get() = try {
        this?.asLong
    } catch (e: RuntimeException) {
        logException(logger, e, "asLong")
        null
    }

val JsonElement?.asDoubleOrNull: Double?
    get() = try {
        this?.asDouble
    } catch (e: RuntimeException) {
        logException(logger, e, "asDouble")
        null
    }

val JsonElement?.asFloatOrNull: Float?
    get() = try {
        this?.asFloat
    } catch (e: RuntimeException) {
        logException(logger, e, "asFloat")
        null
    }

val JsonElement?.asIntOrNull: Int?
    get() = try {
        this?.asInt
    } catch (e: RuntimeException) {
        logException(logger, e, "asInt")
        null
    }

val JsonElement?.asNumberOrNull: Number?
    get() = try {
        this?.asNumber
    } catch (e: RuntimeException) {
        logException(logger, e, "asNumber")
        null
    }

val JsonElement?.asStringOrNull: String?
    get() = try {
        this?.asString
    } catch (e: RuntimeException) {
        logException(logger, e, "asString")
        null
    }

val JsonElement?.asBooleanOrNull: Boolean?
    get() = try {
        this?.asBoolean
    } catch (e: RuntimeException) {
        logException(logger, e, "asBoolean")
        null
    }

val JsonElement?.asJsonObjectOrNull: JsonObject?
    get() = try {
        this?.asJsonObject
    } catch (e: IllegalStateException) {
        logException(logger, e, "asJsonObject")
        null
    }

val JsonElement?.asJsonArrayOrNull: JsonArray?
    get() = try {
        this?.asJsonArray
    } catch (e: IllegalStateException) {
        logException(logger, e, "asJsonArray")
        null
    }

/**
 * @param target имя параметра
 * @return значение параметра или пустая строка при отсутствии значения
 */
@JvmOverloads
fun JsonObject.optStringNonNull(target: String, fallback: String? = EMPTY_STRING): String {
    return if (has(target) && !this[target].isJsonNull) {
        this[target].asString
    } else {
        fallback.orEmpty()
    }
}

fun <E : JsonElement> JsonElement?.asJsonElement(clazz: Class<E>): E? = try {
    asJsonElementOrThrow(clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonElementAs")
    null
}

@Throws(JsonParseException::class)
fun <E : JsonElement> JsonElement?.asJsonElementOrThrow(clazz: Class<E>): E {
    return when {
        this is JsonNull && JsonNull::class.java.isAssignableFrom(clazz) ->
            this as E
        this is JsonPrimitive && JsonPrimitive::class.java.isAssignableFrom(clazz) ->
            this as E
        this is JsonObject && JsonObject::class.java.isAssignableFrom(clazz) ->
            this as E
        this is JsonArray && JsonArray::class.java.isAssignableFrom(clazz) ->
            this as E
        JsonElement::class.java.isAssignableFrom(clazz) -> {
            this as E
        }
        else -> throw JsonParseException("Incorrect type of json element $this")
    }
}

fun <V> JsonElement?.asPrimitiveNonNull(clazz: Class<V>, defaultValue: V): V =
    asPrimitive(clazz, defaultValue) ?: defaultValue

@JvmOverloads
fun <V> JsonElement?.asPrimitive(clazz: Class<V>, defaultValue: V? = null): V? = try {
    asPrimitiveOrThrow(clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonPrimitiveAs")
    defaultValue
}

@Throws(JsonParseException::class)
fun <V> JsonElement?.asPrimitiveOrThrow(clazz: Class<V>): V =
    asJsonElementOrThrow(JsonPrimitive::class.java).asPrimitiveOrThrow(clazz)

fun <V> JsonPrimitive?.asPrimitiveNonNull(clazz: Class<V>, defaultValue: V): V =
    asPrimitive(clazz, defaultValue) ?: defaultValue

@JvmOverloads
fun <V> JsonPrimitive?.asPrimitive(clazz: Class<V>, defaultValue: V? = null): V? = try {
    asPrimitiveOrThrow(clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonPrimitiveAs")
    defaultValue
}

@Throws(JsonParseException::class)
fun <V> JsonPrimitive?.asPrimitiveOrThrow(clazz: Class<V>): V {
    return if (this != null) {
        if (this.isString && String::class.java.isAssignableFrom(clazz)) {
            this.asString as V
        } else if (this.isNumber) {
            val numberValue = this.asNumber
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
                        else -> throw JsonParseException("Json element $this is not known primitive")
                    }
                }
                Number::class.java.isAssignableFrom(clazz) -> {
                    numberValue as V
                }
                else -> {
                    throw JsonParseException("Json element $this is not Number")
                }
            }
        } else if (this.isBoolean && Boolean::class.java.isAssignableFrom(clazz)) {
            this.asBoolean as V
        } else {
            throw JsonParseException("Json element $this is not Boolean")
        }
    } else {
        throw JsonParseException("Json element $this is null")
    }
}

// same as asNumberOrNull getter
fun <P : Number?> JsonElement?.asNumber(): P? = try {
    asNumberOrThrow()
} catch (e: JsonParseException) {
    logException(logger, e, "asNumber")
    null
}

@Throws(JsonParseException::class)
fun <P : Number?> JsonElement?.asNumberOrThrow(): P {
    if (this !is JsonPrimitive) {
        throw JsonParseException("Json element $this is not JsonPrimitive")
    }
    return if (this.isNumber) {
        this.asNumber as P
    } else {
        throw JsonParseException("Json element $this is not Number")
    }
}

fun JsonElement.isJsonFieldNull(memberName: String?): Boolean {
    val obj = this.asJsonElement(JsonObject::class.java) ?: return false
    val element = obj[memberName].asJsonElement(JsonElement::class.java)
    return element == null || element.isJsonNull
}

fun <V> JsonElement?.getPrimitiveNonNull(memberName: String?, clazz: Class<V>, defaultValue: V): V =
    getPrimitive(memberName, clazz) ?: defaultValue

fun <V> JsonElement?.getPrimitive(memberName: String?, clazz: Class<V>): V? = try {
    getPrimitiveOrThrow(memberName, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getPrimitive")
    null
}

@Throws(JsonParseException::class)
fun <V> JsonElement?.getPrimitiveOrThrow(memberName: String?, clazz: Class<V>): V =
    getJsonElementOrThrow(memberName, JsonPrimitive::class.java).asPrimitiveOrThrow(clazz)


fun <E : JsonElement> JsonElement?.getJsonElement(memberName: String?, clazz: Class<E>): E? = try {
    getJsonElementOrThrow(memberName, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "getJsonElement")
    null
}

@Throws(JsonParseException::class)
fun <E : JsonElement> JsonElement?.getJsonElementOrThrow(memberName: String?, clazz: Class<E>): E =
    asJsonElementOrThrow(JsonObject::class.java)[memberName].asJsonElementOrThrow(clazz)


fun JsonArray.getOrNull(index: Int) = try {
    get(index)
} catch (e: IndexOutOfBoundsException) {
    logException(logger, e, "get")
    null
}

fun <V> JsonArray?.parseNonNull(parseElement: (JsonElement) -> V?): List<V> {
    return this?.mapNotNull { parseElement(it) }.orEmpty()
}

fun <V> JsonArray?.parse(parseElement: (JsonElement) -> V?): List<V?> {
    return this?.map { parseElement(it) }.orEmpty()
}

fun <J : JsonElement?> toJsonElement(value: String?, clazz: Class<J>): J? = try {
    toJsonElementOrThrow(value, clazz)
} catch (e: JsonParseException) {
    logException(logger, e, "toJsonElement")
    null
}

@Throws(JsonParseException::class)
fun <J : JsonElement?> toJsonElementOrThrow(value: String?, clazz: Class<J>): J {
    var element: JsonElement? = null
    if (value != null && value.isNotEmpty()) {
        element = parseString(value) // Streams.parse(reader)
    }
    return if (element != null && clazz.isInstance(element)) {
        element as J
    } else {
        throw JsonParseException("Json element $element is not instance of $clazz")
    }
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

fun JsonReader.nextJsonElement(): JsonElement? = try {
    nextJsonElementOrThrow()
} catch (e: JsonParseException) {
    logException(logger, e, "nextJsonElement")
    null
}

@Throws(JsonParseException::class)
fun JsonReader.nextJsonElementOrThrow(): JsonElement = Streams.parse(this)
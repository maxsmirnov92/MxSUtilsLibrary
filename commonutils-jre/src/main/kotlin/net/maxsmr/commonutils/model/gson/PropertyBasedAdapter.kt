package net.maxsmr.commonutils.model.gson

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Регистрировать для абстрактных типов;
 * при сериализации происходит дописывание его имени класса,
 * десериализация делегируется для конкретного известного класса
 *
 * ВНИМАНИЕ!
 * 1. Вероятно не применим, если у абстракции, для которой регистрируется этот адаптер, есть хотя бы 1 enum реализация.
 * В этом случае метод [serialize] для нее не вызывается, и как следствие затем при десериализации не будут найдены
 * метаданные (имя класса), что приводит к крашу.
 * 1. Можно использовать **ТОЛЬКО** для регистрации абстракцых типов - интерфейсов либо абстрактных классов. При
 * попытке зарегистрировать на конкретном типе или open классе возможна бесконечная рекурсия при сериализации (см. доку [serialize])
 */
class PropertyBasedAdapter<T: Any> : JsonSerializer<T>, JsonDeserializer<T> {

    override fun serialize(src: T, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonElement = context.serialize(src, src.javaClass)
        val obj = JsonObject()
        obj.add(KEY_SOURCE, jsonElement)
        obj.addProperty(KEY_CLASS_META, src.javaClass.name)
        return obj
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T? {
        getJsonPrimitive(json, KEY_CLASS_META, String::class.java)?.let { className ->
            val clz: Class<*> = try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                throw JsonParseException(e)
            }
            return context.deserialize(getJsonElement(json, KEY_SOURCE, JsonElement::class.java), clz)
        }
        throw JsonParseException("$KEY_CLASS_META not specified for json: $json")
    }

    companion object {

        const val KEY_SOURCE = "SOURCE"
        const val KEY_CLASS_META = "CLASS_META"
    }
}
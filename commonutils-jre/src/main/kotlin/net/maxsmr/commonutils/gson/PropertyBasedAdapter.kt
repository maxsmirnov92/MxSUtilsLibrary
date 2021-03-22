package net.maxsmr.commonutils.gson

import com.google.gson.*
import net.maxsmr.commonutils.getJsonElement
import net.maxsmr.commonutils.getJsonPrimitive
import java.lang.reflect.Type

/**
 * Регистрировать для абстрактных типов;
 * при сериализации происходит дописывание его имени класса,
 * десериализация делегируется для конкретного известного класса
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
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
        val className: String? = getJsonPrimitive(json, KEY_CLASS_META, String::class.java)
        className?.let  {
            val clz: Class<*> = try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                throw JsonParseException(e)
            }
            return context.deserialize(getJsonElement(json, KEY_SOURCE, JsonElement::class.java), clz)
        }
        throw JsonParseException("$KEY_CLASS_META not specified")
    }

    companion object {

        private const val KEY_SOURCE = "SOURCE"
        private const val KEY_CLASS_META = "CLASS_META"
    }
}
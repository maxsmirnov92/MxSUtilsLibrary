package net.maxsmr.commonutils.gui.actions.message.text

import com.google.gson.*
import net.maxsmr.commonutils.JsonParcelArrayObjects
import net.maxsmr.commonutils.getFromJsonArray
import net.maxsmr.commonutils.getJsonElementAs
import net.maxsmr.commonutils.getJsonPrimitiveFromObject
import net.maxsmr.commonutils.text.EMPTY_STRING
import java.lang.reflect.Type

class TextMessageTypeAdapter<M : TextMessage>(
        private val clazz: Class<M>
) : JsonSerializer<TextMessage>, JsonDeserializer<TextMessage> {

    override fun serialize(src: TextMessage?, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        src?.let {
            if (it is PluralTextMessage) {
                jsonObject.addProperty(KEY_PLURAL_RES_ID, it.pluralResId)
                jsonObject.addProperty(KEY_QUANTITY, it.quantity)
            }
            jsonObject.addProperty(KEY_MESSAGE, it.message?.toString() ?: EMPTY_STRING)
            jsonObject.addProperty(KEY_MESSAGE_RES_ID, it.messageResId)
            jsonObject.add(KEY_ARGS, JsonArray().apply {
                it.args.forEach { arg ->
                    add(context.serialize(arg))
                }
            }
            )
        }
        return jsonObject
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext): TextMessage {
        var pluralResId: Int? = null
        var quantity: Int? = null
        var message: String? = null
        var messageResId: Int? = null
        val args = mutableListOf<TextMessage.Arg<*>>()
        getJsonElementAs(json, JsonObject::class.java)?.let {
            pluralResId = getJsonPrimitiveFromObject(it, KEY_PLURAL_RES_ID, Int::class.java)
            quantity = getJsonPrimitiveFromObject(it, KEY_QUANTITY, Int::class.java)
            message = getJsonPrimitiveFromObject(it, KEY_MESSAGE, String::class.java)
            messageResId = getJsonPrimitiveFromObject(it, KEY_MESSAGE_RES_ID, Int::class.java)
            args.addAll(getFromJsonArray(getJsonElementAs(it[KEY_ARGS], JsonArray::class.java), object : JsonParcelArrayObjects<TextMessage.Arg<*>>() {

                override fun fromJsonObject(jsonObj: JsonObject?): TextMessage.Arg<*>? = context.deserialize(jsonObj, TextMessage.Arg::class.java)

            }, true) as List<TextMessage.Arg<*>>)
        }
        return if (clazz.isAssignableFrom(PluralTextMessage::class.java)) {
            PluralTextMessage(pluralResId ?: 0, quantity ?: 0, args)
        } else {
            TextMessage(message, messageResId, *args.toTypedArray())
        }
    }

    companion object {

        private const val KEY_MESSAGE = "message"
        private const val KEY_MESSAGE_RES_ID = "message_res_id"
        private const val KEY_PLURAL_RES_ID = "plural_res_id"
        private const val KEY_QUANTITY = "quantity"
        private const val KEY_ARGS = "args"
    }
}
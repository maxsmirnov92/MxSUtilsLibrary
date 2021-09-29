package net.maxsmr.commonutils.gui.actions.message.text

import com.google.gson.*
import net.maxsmr.commonutils.gui.actions.message.text.TextMessage.CharSequenceArg
import net.maxsmr.commonutils.model.gson.*
import java.lang.reflect.Type

class TextMessageTypeAdapter : JsonSerializer<TextMessage>, JsonDeserializer<TextMessage> {

    override fun serialize(src: TextMessage?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        src?.let {
            if (it is PluralTextMessage) {
                jsonObject.addProperty(KEY_PLURAL_RES_ID, it.pluralResId)
                jsonObject.addProperty(KEY_QUANTITY, it.quantity)
            }
            it.message?.let { message ->
                jsonObject.addProperty(KEY_MESSAGE, message.toString())
            }
            it.messageResId?.let { messageResId ->
                jsonObject.addProperty(KEY_MESSAGE_RES_ID, messageResId)
            }
            jsonObject.add(KEY_ARGS, JsonArray().apply {
                it.args.forEach { arg ->
                    add(context.serialize(arg, TextMessage.Arg::class.java))
                }
            }
            )
        }
        return jsonObject
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): TextMessage {
        var pluralResId: Int? = null
        var quantity: Int? = null
        var message: String? = null
        var messageResId: Int? = null
        val args = mutableListOf<TextMessage.Arg<*>>()
        getJsonElementAs(json, JsonObject::class.java)?.let {
            pluralResId = getJsonPrimitive(it, KEY_PLURAL_RES_ID, Int::class.java)
            quantity = getJsonPrimitive(it, KEY_QUANTITY, Int::class.java)
            message = getJsonPrimitive(it, KEY_MESSAGE, String::class.java)
            messageResId = getJsonPrimitive(it, KEY_MESSAGE_RES_ID, Int::class.java)
            args.addAll(getFromJsonArray(getJsonElementAs(it[KEY_ARGS], JsonArray::class.java), object : JsonParcelArrayObjects<TextMessage.Arg<*>>() {

                override fun fromJsonObject(jsonObj: JsonObject): TextMessage.Arg<*>? = context.deserialize(jsonObj, TextMessage.Arg::class.java)

            }, true) as List<TextMessage.Arg<*>>)
        }
        return if (typeOfT is Class<*> && PluralTextMessage::class.java.isAssignableFrom(typeOfT)) {
            PluralTextMessage(pluralResId ?: 0, quantity ?: 0, *args.toTypedArray())
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

class CharSequenceArgTypeAdapter : JsonSerializer<CharSequenceArg>, JsonDeserializer<CharSequenceArg> {

    override fun serialize(src: CharSequenceArg?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
            JsonObject().apply {
                src?.let {
                    addProperty(KEY_VALUE, it.value.toString())
                }
            }

    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): CharSequenceArg? =
            getJsonPrimitive(json, KEY_VALUE, String::class.java)?.let {
                CharSequenceArg(it)
            }


    companion object {

        private const val KEY_VALUE = "value"
    }
}
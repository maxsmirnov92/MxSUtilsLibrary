package net.maxsmr.commonutils.gui.message

import com.google.gson.*
import net.maxsmr.commonutils.gui.message.TextMessage.CharSequenceArg
import net.maxsmr.commonutils.model.gson.*
import java.lang.reflect.Type

class TextMessageTypeAdapter : JsonSerializer<TextMessage>, JsonDeserializer<TextMessage> {

    override fun serialize(src: TextMessage?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        src?.let {
            when (it) {
                is PluralTextMessage -> {
                    jsonObject.addProperty(KEY_PLURAL_RES_ID, it.pluralResId)
                    jsonObject.addProperty(KEY_QUANTITY, it.quantity)
                }
                is JoinTextMessage -> {
                    jsonObject.addProperty(KEY_DIVIDER_RES_ID, it.dividerResId)
                    jsonObject.add(KEY_PARTS_TO_JOIN, JsonArray().apply {
                        it.partsToJoin.forEach { arg ->
                            add(context.serialize(arg, TextMessage.Arg::class.java))
                        }
                    })
                }
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
            })
        }
        return jsonObject
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): TextMessage {
        var pluralResId: Int? = null
        var quantity: Int? = null
        var dividerResId: Int? = null
        val partsToJoin = mutableListOf<TextMessage.Arg<*>>()
        var message: String? = null
        var messageResId: Int? = null
        val args = mutableListOf<TextMessage.Arg<*>>()
        json?.asJsonObjectOrNull?.let {
            pluralResId = it[KEY_PLURAL_RES_ID]?.asIntOrNull
            quantity = it[KEY_QUANTITY]?.asIntOrNull
            dividerResId = it[KEY_DIVIDER_RES_ID]?.asIntOrNull
            partsToJoin.addAll(it[KEY_PARTS_TO_JOIN]?.asJsonArrayOrNull.parseNonNull {
                context.deserialize<TextMessage.Arg<*>>(it.asJsonObjectOrNull, TextMessage.Arg::class.java)
            })
            message = it[KEY_MESSAGE]?.asStringOrNull
            messageResId = it[KEY_MESSAGE_RES_ID]?.asIntOrNull
            args.addAll(it[KEY_ARGS]?.asJsonArrayOrNull.parseNonNull {
                context.deserialize<TextMessage.Arg<*>>(it.asJsonObjectOrNull, TextMessage.Arg::class.java)
            })
        }
        return if (typeOfT is Class<*> && PluralTextMessage::class.java.isAssignableFrom(typeOfT)) {
            PluralTextMessage(pluralResId ?: 0, quantity ?: 0, *args.toTypedArray()).apply {
                message?.let {
                    _setMessage(it)
                }
                messageResId?.let {
                    _setMessageResId(it)
                }
            }
        } else if (typeOfT is Class<*> && JoinTextMessage::class.java.isAssignableFrom(typeOfT)) {
            JoinTextMessage(dividerResId ?: 0, *partsToJoin.toTypedArray())
        } else {
            TextMessage(message, messageResId, *args.toTypedArray())
        }
    }

    companion object {

        private const val KEY_MESSAGE = "message"
        private const val KEY_MESSAGE_RES_ID = "message_res_id"
        private const val KEY_ARGS = "args"
        private const val KEY_PLURAL_RES_ID = "plural_res_id"
        private const val KEY_QUANTITY = "quantity"
        private const val KEY_DIVIDER_RES_ID = "divider_res_id"
        private const val KEY_PARTS_TO_JOIN = "parts_to_join"
    }
}

class CharSequenceArgTypeAdapter : JsonSerializer<CharSequenceArg>, JsonDeserializer<CharSequenceArg> {

    override fun serialize(src: CharSequenceArg?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
        JsonObject().apply {
            src?.let {
                addProperty(KEY_VALUE, it.value.toString())
            }
        }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type,
        context: JsonDeserializationContext,
    ): CharSequenceArg? =
        json?.asJsonObjectOrNull?.get(KEY_VALUE)?.asStringOrNull?.let {
            CharSequenceArg(it)
        }


    companion object {

        private const val KEY_VALUE = "value"
    }
}
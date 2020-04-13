package net.maxsmr.commonutils.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import static net.maxsmr.commonutils.data.gson.GsonUtilsKt.getJsonElementAs;
import static net.maxsmr.commonutils.data.gson.GsonUtilsKt.getJsonPrimitive;

public class PropertyBasedAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    private static final String CLASS_META_KEY = "CLASS_META_KEY";

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement jsonElem = context.serialize(src, src.getClass());
        JsonObject jsonObject = getJsonElementAs(jsonElem, JsonObject.class);
        if (jsonObject != null) {
            jsonObject.addProperty(CLASS_META_KEY, src.getClass().getCanonicalName());
        }
        return jsonElem;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String className = getJsonPrimitive(json, CLASS_META_KEY, String.class);
        if (className != null) {
            Class<?> clz;
            try {
                clz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
            return context.deserialize(json, clz);
        }
        throw new JsonParseException(CLASS_META_KEY + " not specified");
    }

}
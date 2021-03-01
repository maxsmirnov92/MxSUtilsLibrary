package net.maxsmr.commonutils.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import static net.maxsmr.commonutils.GsonUtilsKt.getJsonElement;
import static net.maxsmr.commonutils.GsonUtilsKt.getJsonPrimitive;

public class PropertyBasedAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    private static final String KEY_SOURCE = "SOURCE";
    private static final String KEY_CLASS_META = "CLASS_META";

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement jsonElem = context.serialize(src, src.getClass());
        JsonObject object = new JsonObject();
        object.add(KEY_SOURCE, jsonElem);
        object.addProperty(KEY_CLASS_META, src.getClass().getCanonicalName());
        return object;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String className = getJsonPrimitive(json, KEY_CLASS_META, String.class);
        if (className != null) {
            Class<?> clz;
            try {
                clz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
            return context.deserialize(getJsonElement(json, KEY_SOURCE, JsonElement.class), clz);
        }
        throw new JsonParseException(KEY_CLASS_META + " not specified");
    }
}
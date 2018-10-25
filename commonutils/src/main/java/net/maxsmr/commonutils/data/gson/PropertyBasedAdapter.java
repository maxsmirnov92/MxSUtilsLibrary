package net.maxsmr.commonutils.data.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class PropertyBasedAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

//    @NotNull
//    private final Class<T> mClass;
//
//    public PropertyBasedAdapter(@NotNull Class<T> clazz) {
//        mClass = clazz;
//    }

    private static final String CLASS_META_KEY = "CLASS_META_KEY";

    @Override
    public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement jsonElem = context.serialize(src, src.getClass());
        jsonElem.getAsJsonObject().addProperty(CLASS_META_KEY,
                src.getClass().getCanonicalName());
        return jsonElem;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObj = json.getAsJsonObject();
        String className = jsonObj.get(CLASS_META_KEY).getAsString();
        try {
            Class<?> clz = Class.forName(className);
            return context.deserialize(json, clz);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

}
package net.maxsmr.commonutils.data.gson;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GsonHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(GsonHelper.class);

    private GsonHelper() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    public static <T> T fromJsonObjectString(@NotNull Gson gson, @Nullable String jsonString, @NotNull Class<T> classOfT) {
        return fromJsonObjectString(gson, jsonString, (Type) classOfT);
    }

    @Nullable
    public static <T> T fromJsonObjectString(@NotNull Gson gson, @Nullable String jsonString, @NotNull TypeToken<T> typeToken) {
        return fromJsonObjectString(gson, jsonString, typeToken.getType());
    }

    @Nullable
    private static <T> T fromJsonObjectString(@NotNull Gson gson, @Nullable String jsonString, @NotNull Type type) {
        try {
            return gson.fromJson(jsonString, type);
        } catch (JsonParseException e) {
            logger.e("an JsonParseException occurred during fromJson(): " + e.getMessage(), e);
        }
        return null;
    }

    @NotNull
    public static <T> List<T> fromJsonArrayString(@NotNull Gson gson, @Nullable String jsonString, @NotNull Class<T[]> type) {
        T[] array = null;
        try {
            array = gson.fromJson(jsonString, type);
        } catch (JsonParseException e) {
            logger.e("an JsonParseException occurred during fromJson(): " + e.getMessage(), e);
        }
        if (array != null) {
            return new ArrayList<>(Arrays.asList(array));
        }
        return Collections.emptyList();
    }

    @NotNull
    public static <T> String toJsonString(@NotNull Gson gson, T object) {
        try {
            return gson.toJson(object);
        } catch (JsonParseException e) {
            logger.e("an JsonParseException occurred during toJson(): " + e.getMessage(), e);
        }
        return "null";
    }

    @NotNull
    public static <T> Map<T, String> toJsonStringMap(@NotNull Gson gson, @Nullable Collection<T> listOfObjects) {
        Map<T, String> result = new LinkedHashMap<>();
        if (listOfObjects != null) {
            for (T o : listOfObjects) {
                result.put(o, toJsonString(gson, o));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <P extends Number> P getPrimitiveNumber(Object object, Class<P> clazz) {
        return object != null && clazz.isAssignableFrom(object.getClass()) ? (P) object : (P) Integer.valueOf(0);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <P extends Number> P getPrimitiveNumber(JsonElement element, Class<P> clazz) {
        JsonPrimitive obj = element instanceof JsonPrimitive ? (JsonPrimitive) element : null;
        return obj != null && obj.isNumber() && clazz.isAssignableFrom(obj.getAsNumber().getClass()) ? (P) obj.getAsNumber() : (P) Integer.valueOf(0);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static String getPrimitiveString(Object object) {
        return object instanceof String ? (String) object : null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static String getPrimitiveString(JsonElement object) {
        JsonPrimitive obj = object instanceof JsonPrimitive ? (JsonPrimitive) object : null;
        return obj != null && obj.isString() ? obj.getAsString() : null;
    }

    @SuppressWarnings("unchecked")
    public static boolean getPrimitiveBoolean(Object object) {
        return object instanceof Boolean ? (Boolean) object : false;
    }

    @SuppressWarnings("unchecked")
    public static boolean getPrimitiveBoolean(JsonElement object) {
        JsonPrimitive obj = object instanceof JsonPrimitive ? (JsonPrimitive) object : null;
        return (obj != null && obj.isBoolean()) && obj.getAsBoolean();
    }

    @Nullable
    public static <V> V getJsonPrimitiveValueIn(@Nullable JsonElement inElement, String memberName, Class<V> clazz) {
        V value = null;
        if (inElement != null) {
            if (inElement.isJsonObject()) {
                JsonObject object = inElement.getAsJsonObject();
                value = getJsonPrimitiveValueFor(object.get(memberName), clazz);
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <V> V getJsonPrimitiveValueFor(@Nullable JsonElement forElement, Class<V> clazz) {
        V value = null;
        if (forElement != null && forElement.isJsonPrimitive()) {
            JsonPrimitive primitive = forElement.getAsJsonPrimitive();
            if (primitive.isString() && String.class.isAssignableFrom(clazz)) {
                value = (V) primitive.getAsString();
            } else if (primitive.isNumber() && Number.class.isAssignableFrom(clazz)) {
                value = (V) primitive.getAsNumber();
            } else if (primitive.isBoolean() && Boolean.class.isAssignableFrom(clazz)) {
                value = (V) Boolean.valueOf(primitive.getAsBoolean());
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <J extends JsonElement> J asJsonElement(@Nullable String string, @NotNull Class<J> clazz) {
        JsonParser parser = new JsonParser();
        JsonElement element = null;
        if (!TextUtils.isEmpty(string)) {
            try {
                element = parser.parse(string);
            } catch (JsonParseException e) {
                logger.e("an JsonParseException occurred during parse(): " + e.getMessage(), e);
            }
        }
        if (element != null && element.getClass().isAssignableFrom(clazz)) {
            return (J) element;
        }
        return null;
    }
}

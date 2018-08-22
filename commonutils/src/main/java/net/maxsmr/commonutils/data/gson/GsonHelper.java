package net.maxsmr.commonutils.data.gson;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GsonHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(GsonHelper.class);

    private GsonHelper() {
        throw new AssertionError("no instances.");
    }

    /**
     * @param type T Serializable or Parcelable
     */
    @Nullable
    public static <T> T fromJsonObjectString(@NonNull Gson gson, @Nullable String jsonString, @NonNull Class<T> type) {
        logger.d("fromJsonObjectString(), jsonString=" + jsonString + ", type=" + type);
        try {
            return gson.fromJson(jsonString, type);
        } catch (Exception e) {
            logger.e("an Exception occurred during fromJson(): " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param type T Serializable or Parcelable
     */
    @NonNull
    public static <T> List<T> fromJsonArrayString(@NonNull Gson gson, @Nullable String jsonString, @NonNull Class<T[]> type) {
        logger.d("fromJsonArrayString(), jsonString=" + jsonString + ", type=" + type);
        try {
            return new ArrayList<>(Arrays.asList(gson.fromJson(jsonString, type)));
        } catch (Exception e) {
            logger.e("an Exception occurred during fromJson(): " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @NonNull
    public static <T> String toJsonString(@NonNull Gson gson, T... what) {
        logger.d("toJsonString(), what=" + Arrays.toString(what));

        try {
            return gson.toJson(what != null && what.length == 1? what[0] : what);
        } catch (Exception e) {
            logger.e("an Exception occurred during toJson(): " + e.getMessage(), e);
            return "null";
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <P extends Number> P getPrimitiveNumber(Object object, Class<P> clazz) {
        return object != null && clazz.isAssignableFrom(object.getClass()) ? (P) object : (P) Integer.valueOf(0);
    }

    @SuppressWarnings("unchecked")
    @NonNull
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
            if (primitive.isString() && clazz.isAssignableFrom(String.class)) {
                value = (V) primitive.getAsString();
            } else if (primitive.isNumber() && clazz.isAssignableFrom(Number.class)) {
                value = (V) primitive.getAsNumber();
            }
        }
        return value;
    }
}

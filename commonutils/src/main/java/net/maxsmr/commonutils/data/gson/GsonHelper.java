package net.maxsmr.commonutils.data.gson;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GsonHelper {

    private static final Logger logger = LoggerFactory.getLogger(GsonHelper.class);

    private GsonHelper() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    public static <T extends Serializable> T fromJsonObjectString(@Nullable String jsonString, @NonNull Class<T> type) {
        logger.debug("fromJsonObjectString(), jsonString=" + jsonString + ", type=" + type);

        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        try {
            return gson.fromJson(jsonString, type);
        } catch (Exception e) {
            logger.error("an Exception occurred during fromJson(): " + e.getMessage());
            return null;
        }
    }

    @NonNull
    public static <T extends Serializable> List<T> fromJsonArrayString(@Nullable String jsonString, @NonNull Class<T[]> type) {
        logger.debug("fromJsonArrayString(), jsonString=" + jsonString + ", type=" + type);

        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        try {
            return new ArrayList<>(Arrays.asList(gson.fromJson(jsonString, type)));
        } catch (Exception e) {
            logger.error("an Exception occurred during fromJson(): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @NonNull
    public static <T extends Serializable> String toJsonString(T... what) {
        logger.debug("toJsonString(), what=" + Arrays.toString(what));

        final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        try {
            return gson.toJson(what);
        } catch (Exception e) {
            logger.error("an Exception occurred during toJson(): " + e.getMessage());
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
        return obj != null && clazz.isAssignableFrom(obj.getAsNumber().getClass()) ? (P) obj.getAsNumber() : (P) Integer.valueOf(0);
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
}

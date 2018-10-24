package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

    /**
     * Finds method in class by iterating the available methods
     * CAUTION: Do not use this for overloaded methods!
     */
    public static Method findMethod(@NonNull Class<?> clazz, String methodName) throws NoSuchMethodException {
        if (TextUtils.isEmpty(methodName)) {
            throw new IllegalArgumentException("Nethod name is empty");
        }
        for (Method method : clazz.getMethods()) {
            if (methodName.equals(method.getName())) {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            }
        }
        throw new NoSuchMethodException("Method '" + methodName + "' not found in class " + clazz.getName());
    }

    /** @param callObject null if field is static */
    @SuppressWarnings("unchecked")
    public static <T, O> T getFieldValue(@NonNull Class<O> classOfO, @Nullable O callObject, String fieldName) throws RuntimeException, NoSuchFieldException {
        if (TextUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name is empty");
        }
        final Field targetField = classOfO.getDeclaredField(fieldName);
        if (targetField != null) {
            if (!targetField.isAccessible()) {
                targetField.setAccessible(true);
            }
            try {
                return (T) targetField.get(callObject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in class " + classOfO.getName());
    }

}

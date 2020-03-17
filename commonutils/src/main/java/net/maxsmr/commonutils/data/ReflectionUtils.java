package net.maxsmr.commonutils.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.maxsmr.commonutils.data.CompareUtilsKt.objectsEqual;
import static net.maxsmr.commonutils.data.CompareUtilsKt.stringsEqual;
import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;

public class ReflectionUtils {

    /**
     * Finds method in class by iterating the available methods
     * CAUTION: Do not use this for overloaded methods!
     */
    @Nullable
    public static Method findMethod(
            @NotNull Class<?> clazz,
            String methodName,
            boolean ignoreCase,
            boolean makeAccessible
    ) {
        for (Method method : clazz.getMethods()) {
            if (stringsEqual(methodName, method.getName(), ignoreCase)) {
                if (makeAccessible && !method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            }
        }
        return null;
    }

    /**
     * @param callObject null if field is static
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T, O> T getFieldValue(
            @NotNull Class<O> classOfObject,
            @Nullable O callObject,
            String fieldName
    ) throws RuntimeException {
        if (isEmpty(fieldName)) {
            throw new IllegalArgumentException("Field name is empty");
        }
        Field resultField;
        Exception resultException = null;
        try {
            resultField = classOfObject.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            resultField = null;
            resultException = e;
        }
        if (resultField != null) {
            if (!resultField.isAccessible()) {
                resultField.setAccessible(true);
            }
            try {
                // or 'isInstance'
                return (T) resultField.get(callObject);
            } catch (IllegalAccessException | ClassCastException e) {
                resultException = e;
            }
        }
        throw new RuntimeException(resultException);
    }

    public static boolean checkFieldValueExists(
            Object fieldValue,
            String fieldName,
            @NotNull Class<?> type,
            boolean ignoreCase
    ) {

        Field[] fields = type.getDeclaredFields();

        if (fields.length == 0)
            return false;

        for (Field field : fields) {
            field.setAccessible(true);
            if (stringsEqual(fieldName, field.getName(), ignoreCase)) {
                try {
                    if (objectsEqual(field.get(null), fieldValue))
                        return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

}

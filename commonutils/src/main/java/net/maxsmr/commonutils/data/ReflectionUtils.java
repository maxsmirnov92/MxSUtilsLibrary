package net.maxsmr.commonutils.data;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.maxsmr.commonutils.data.CompareUtilsKt.objectsEqual;
import static net.maxsmr.commonutils.data.CompareUtilsKt.stringsEqual;
import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;
import static net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.formatException;

public class ReflectionUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ReflectionUtils.class);

    @Nullable
    public static Method findMethod(
            @NotNull Class<?> clazz,
            String methodName,
            boolean ignoreCase,
            boolean makeAccessible
    ) {
        try {
            return findMethodOrThrow(clazz, methodName, ignoreCase, makeAccessible);
        } catch (RuntimeException e) {
            logger.e(e);
            return null;
        }
    }

    /**
     * Finds method in class by iterating the available methods
     * CAUTION: Do not use this for overloaded methods!
     */
    @Nullable
    public static Method findMethodOrThrow(
            @NotNull Class<?> clazz,
            String methodName,
            boolean ignoreCase,
            boolean makeAccessible
    ) throws RuntimeException {
        try {
            for (Method method : clazz.getMethods()) {
                if (stringsEqual(methodName, method.getName(), ignoreCase)) {
                    if (makeAccessible && !method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    return method;
                }
            }
        } catch (SecurityException e) {
            throw new RuntimeException(formatException(e), e);
        }
        return null;
    }

    @Nullable
    public static <T> T invokeMethod(
            @NotNull Class<?> clazz,
            String methodName,
            Class<?>[] parameterTypes,
            Object invokeObject,
            Object... invokeArgs
    ) {
        try {
            return invokeMethodOrThrow(clazz, methodName, parameterTypes, invokeObject, invokeArgs);
        } catch (RuntimeException e) {
            logger.e(e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T invokeMethodOrThrow(
            @NotNull Class<?> clazz,
            String methodName,
            Class<?>[] parameterTypes,
            Object invokeObject,
            Object... invokeArgs
    ) throws RuntimeException {
        Method method;
        try {
            method = clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(formatException(e, "getMethod"), e);
        }
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return (T) method.invoke(invokeObject, invokeArgs);
        } catch (Exception e) {
            throw new RuntimeException(formatException(e, "invoke"), e);
        }
    }

    @Nullable
    public static <T, O> T getFieldValue(
            @NotNull Class<O> classOfObject,
            @Nullable O callObject,
            String fieldName
    ) {
        try {
            return getFieldValueOrThrow(classOfObject, callObject, fieldName);
        } catch (RuntimeException e) {
            logger.e(e);
            return null;
        }
    }

    /**
     * @param callObject null if field is static
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T, O> T getFieldValueOrThrow(
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
            try {
                if (!resultField.isAccessible()) {
                    resultField.setAccessible(true);
                }
                // or 'isInstance'
                return (T) resultField.get(callObject);
            } catch (Exception e) {
                resultException = e;
            }
        }
        throw new RuntimeException(formatException(resultException), resultException);
    }

    public static boolean checkFieldValueExists(
            Object fieldValue,
            String fieldName,
            @NotNull Class<?> type,
            boolean ignoreCase
    ) {
        try {
            return checkFieldValueExistsOrThrow(fieldValue, fieldName, type, ignoreCase);
        } catch (RuntimeException e) {
            logger.e(e);
            return false;
        }
    }

    public static boolean checkFieldValueExistsOrThrow(
            Object fieldValue,
            String fieldName,
            @NotNull Class<?> type,
            boolean ignoreCase
    ) throws RuntimeException {

        try {
            Field[] fields = type.getDeclaredFields();

            if (fields.length == 0) {
                return false;
            }

            for (Field field : fields) {
                field.setAccessible(true);
                if (stringsEqual(fieldName, field.getName(), ignoreCase)) {
                    if (objectsEqual(field.get(null), fieldValue))
                        return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(formatException(e), e);
        }

        return false;
    }
}

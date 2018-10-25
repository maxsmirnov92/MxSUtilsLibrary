package net.maxsmr.commonutils.data;

import android.text.TextUtils;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ReflectionUtils.class);

    /**
     * Finds method in class by iterating the available methods
     * CAUTION: Do not use this for overloaded methods!
     */
    public static Method findMethod(@NotNull Class<?> clazz, String methodName) throws NoSuchMethodException {
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
    public static <T, O> T getFieldValue(@NotNull Class<O> classOfO, @Nullable O callObject, String fieldName) throws RuntimeException, NoSuchFieldException {
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

    /**
     * @param id      the constant value of resource subclass field
     * @param resType subclass where the static final field with given id value declared
     */
    public static boolean checkResourceIdExists(int id, String resName, Class<?> resType) throws NullPointerException, IllegalArgumentException, IllegalAccessException {

        if (resType == null)
            throw new NullPointerException("resType is null");

        Field[] fields = resType.getDeclaredFields();

        if (fields.length == 0)
            return false;

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals(resName)) {
                try {
                    if (CompareUtils.objectsEqual(field.getInt(null), id))
                        return true;
                } catch (Exception e) {
                    logger.e("an Exception occurred during getInt()");
                }
            }
        }

        return false;
    }


}

package net.maxsmr.commonutils.gson.exclusion;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import org.jetbrains.annotations.Nullable;

/** @see net.maxsmr.commonutils.data.gson.exclusion.FieldExclude */
public class FieldsAnnotationExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(@Nullable FieldAttributes f) {
        if (f != null) {
            return f.getAnnotation(FieldExclude.class) != null;
        }
        return false;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
package net.maxsmr.commonutils.data.gson.exclusion;

import android.support.annotation.Nullable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/** @see net.maxsmr.commonutils.data.gson.exclusion.FieldExclude */
class FieldsAnnotationExclusionStrategy implements ExclusionStrategy {

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
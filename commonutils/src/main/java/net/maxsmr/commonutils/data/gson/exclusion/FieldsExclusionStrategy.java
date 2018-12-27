package net.maxsmr.commonutils.data.gson.exclusion;

import org.jetbrains.annotations.NotNull;
import android.text.TextUtils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import net.maxsmr.commonutils.data.Predicate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class FieldsExclusionStrategy implements ExclusionStrategy {

    private final Set<Class<?>> classesToCheck ;

    @NotNull
    private final Set<String> fieldNamesToCheck;

    public FieldsExclusionStrategy(Collection<Class<?>> classesToCheck, Collection<String> fieldNamesToCheck) {
        this.classesToCheck = classesToCheck != null? new LinkedHashSet<>(classesToCheck) : Collections.emptySet();
        this.fieldNamesToCheck = fieldNamesToCheck != null? new LinkedHashSet<>(fieldNamesToCheck) : Collections.emptySet();
        if (this.fieldNamesToCheck.isEmpty()) {
            throw new IllegalArgumentException("must contain at least one field name");
        }
    }

    public Set<Class<?>> getClassesToCheck() {
        return Collections.unmodifiableSet(classesToCheck);
    }

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        if (f == null) {
            return true;
        }
        Class<?> clazz = f.getDeclaringClass();
        return clazz == null || (TextUtils.isEmpty(f.getName()) || Predicate.Methods.contains(fieldNamesToCheck, element -> element != null && element.equals(f.getName()))) ||
                (classesToCheck == null || Predicate.Methods.contains(classesToCheck, clazz::equals));
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}

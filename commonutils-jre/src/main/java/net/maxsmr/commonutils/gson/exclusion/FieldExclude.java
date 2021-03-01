package net.maxsmr.commonutils.gson.exclusion;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/** Excluding fields from Gson serialization to json via [FieldsExclusionStrategy] where transient can not be used,
 * e.g. at [android.arch.persistence.room.Entity] */
@Target(FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldExclude {

}
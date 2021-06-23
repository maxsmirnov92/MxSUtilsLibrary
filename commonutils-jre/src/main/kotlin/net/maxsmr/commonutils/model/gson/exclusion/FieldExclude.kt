package net.maxsmr.commonutils.model.gson.exclusion

/** Excluding fields from Gson serialization to json via [FieldsExclusionStrategy] where transient can not be used,
 * e.g. at [android.arch.persistence.room.Entity]  */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FieldExclude
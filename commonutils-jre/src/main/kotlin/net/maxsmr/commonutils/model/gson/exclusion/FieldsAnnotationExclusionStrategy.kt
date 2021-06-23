package net.maxsmr.commonutils.model.gson.exclusion

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

class FieldsAnnotationExclusionStrategy : ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes?): Boolean {
        return if (f != null) {
            f.getAnnotation(FieldExclude::class.java) != null
        } else {
            false
        }
    }

    override fun shouldSkipClass(clazz: Class<*>?): Boolean = false
}
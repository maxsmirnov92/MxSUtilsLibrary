package net.maxsmr.commonutils.model.gson.exclusion

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import net.maxsmr.commonutils.Predicate
import net.maxsmr.commonutils.text.isEmpty

class FieldsExclusionStrategy(
        private val classesToCheck: Set<Class<*>>,
        private val fieldNamesToCheck: Set<String>
) : ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes?): Boolean {
        if (f == null) {
            return true
        }
        val clazz = f.declaringClass
        return clazz == null || isEmpty(f.name)
                || Predicate.Methods.contains(fieldNamesToCheck) { element: String? -> element != null && element == f.name }
                || Predicate.Methods.contains(classesToCheck) { o: Class<*>? -> clazz == o }
    }

    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
        return false
    }
}

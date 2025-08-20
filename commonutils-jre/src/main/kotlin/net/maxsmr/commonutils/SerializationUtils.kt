package net.maxsmr.commonutils

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable

val serializationLogger: BaseLogger = BaseLoggerHolder.instance.getLogger("SerializationExt")

inline fun <reified T : Serializable> ByteArray.asObject(): T? = try {
    asObjectOrThrow()
} catch (e: Exception) {
    logException(serializationLogger, e, "asObject")
    null
}

inline fun <reified T : Serializable> ByteArray.asObjectOrThrow(): T? {
    if (this.isNotEmpty()) {
        return ByteArrayInputStream(this).readObjectOrThrow()
    }
    return null
}

inline fun <reified T : Serializable> InputStream.readObject(): T? = try {
    readObjectOrThrow()
} catch (e: Exception) {
    logException(serializationLogger, e, "readObject")
    null
}

inline fun <reified T : Serializable> InputStream.readObjectOrThrow(): T? {
    ObjectInputStream(this).use {
        val o = it.readObject()
        if (o != null) {
            if (o is T) {
                return o
            } else {
                throw RuntimeException("Incorrect object class: ${o.javaClass}")
            }
        }
    }
    return null
}

fun <T : Serializable> T.asByteArray(): ByteArray? = try {
    asByteArrayOrThrow()
} catch (e: Exception) {
    logException(serializationLogger, e, "asByteArray")
    null
}

fun <T : Serializable> T.asByteArrayOrThrow(): ByteArray {
    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).use {
        it.writeObject(this)
        return bos.toByteArray()
    }
}

fun <T : Serializable> OutputStream.writeObject(obj: T): Boolean = try {
    writeObjectOrThrow(obj)
    true
} catch (e: Exception) {
    logException(serializationLogger, e, "writeObject")
    false
}

fun <T : Serializable> OutputStream.writeObjectOrThrow(obj: T) {
    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).use {
        it.writeObject(obj)
        bos.writeTo(this)
    }
}
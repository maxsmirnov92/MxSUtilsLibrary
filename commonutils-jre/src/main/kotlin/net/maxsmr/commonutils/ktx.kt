package net.maxsmr.commonutils

fun Any?.isNull(): Boolean = this == null
fun Any?.isNotNull(): Boolean = this.isNull().not()

inline fun <T : Any?> Boolean?.isTrue(block: () -> T): T? =
        this?.let { if (this) block() else null }

inline fun <T : Any?> Boolean?.isFalse(block: () -> T): T? =
        this?.let { if (!this) block() else null }

inline fun <T : Any?> Any?.ifNull(block: () -> T): T? = if (this == null) block.invoke() else null

inline fun <T : Any?> Any?.ifNotNull(block: () -> T): T? = if (this != null) block.invoke() else null
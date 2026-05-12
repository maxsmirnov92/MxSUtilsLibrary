package net.maxsmr.commonutils.stream

import java.io.IOException

class StreamCancellationException(
    message: String? = null,
    cause: Exception? = null
) : IOException(message, cause)
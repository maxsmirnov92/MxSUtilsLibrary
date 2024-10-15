package net.maxsmr.commonutils.stream

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.min

class ByteBufferBackedInputStream(private val buf: ByteBuffer) : InputStream() {

    @Throws(IOException::class)
    override fun read(): Int {
        if (!buf.hasRemaining()) {
            return -1
        }
        return buf.get().toInt() and 0xFF
    }

    @Throws(IOException::class)
    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        var len = len
        if (!buf.hasRemaining()) {
            return -1
        }
        len = min(len.toDouble(), buf.remaining().toDouble()).toInt()
        buf[bytes, off, len]
        return len
    }
}
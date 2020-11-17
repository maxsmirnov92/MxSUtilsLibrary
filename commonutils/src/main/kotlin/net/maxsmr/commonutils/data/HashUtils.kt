package net.maxsmr.commonutils.data

import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.android.media.isContentScheme
import net.maxsmr.commonutils.android.media.isFileScheme
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest

const val DEFAULT_ALGORITHM = "SHA-1"

fun digest(
        context: Context,
        uri: Uri,
        algorithm: String = DEFAULT_ALGORITHM
): ByteArray? = digest(context, uri, MessageDigest.getInstance(algorithm))

fun digest(
        context: Context,
        uri: Uri,
        algorithm: MessageDigest
): ByteArray? {
    val stream =
            try {
                when {
                    uri.isFileScheme() -> {
                        FileInputStream(uri.path)
                    }
                    uri.isContentScheme() -> {
                        context.contentResolver.openInputStream(uri)
                    }
                    else -> null
                }
            } catch (e: IOException) {
                null
            }
    stream?.let {
        return digest(stream, algorithm, true)
    }
    return null
}

fun digest(
        `is`: InputStream,
        algorithm: String = DEFAULT_ALGORITHM,
        closeStream: Boolean = true
): ByteArray? = digest(`is`, MessageDigest.getInstance(algorithm), closeStream)

fun digest(
        `is`: InputStream,
        algorithm: MessageDigest,
        closeStream: Boolean = true
): ByteArray? {
    algorithm.reset()
    val bis = BufferedInputStream(`is`)
    val dis = DigestInputStream(bis, algorithm)
    try {
        while (dis.read() != -1) {
        }
    } catch (ignored: IOException) {
    } finally {
        if (closeStream) {
            try {
                `is`.close()
            } catch (ignored: IOException) {
            }
        }
    }
    return algorithm.digest()
}
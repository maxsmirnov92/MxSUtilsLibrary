package net.maxsmr.commonutils.android.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import net.maxsmr.commonutils.android.isAtLeastMarshmallow
import net.maxsmr.commonutils.data.isFileValid
import net.maxsmr.commonutils.data.text.EMPTY_STRING
import net.maxsmr.commonutils.data.text.isEmpty
import net.maxsmr.commonutils.graphic.GraphicUtils
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.io.File
import java.io.FileDescriptor
import java.util.concurrent.TimeUnit

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>("MetadataRetriever")

@JvmOverloads
fun createMediaMetadataRetriever(resourceUri: Uri?, headers: Map<String, String>? = null): MediaMetadataRetriever? {
    return try {
        createMediaMetadataRetrieverOrThrow(resourceUri, headers)
    } catch (e: RuntimeException) {
        logger.e("a RuntimeException occurred during createMediaMetadataRetriever()", e)
        null
    }
}

@Throws(RuntimeException::class)
@JvmOverloads
fun createMediaMetadataRetrieverOrThrow(
        resourceUri: Uri?,
        headers: Map<String, String>? = null
): MediaMetadataRetriever {
    if (resourceUri == null) {
        throw NullPointerException("resourceUri is null")
    }
    val retriever = MediaMetadataRetriever()
    try {
        if (resourceUri.isFileScheme()) {
            retriever.setDataSource(resourceUri.path)
        } else {
            retriever.setDataSource(resourceUri.toString(), headers ?: emptyMap())
        }
    } catch (e: RuntimeException) {
        throw RuntimeException("Cannot set data source $resourceUri", e)
    }

    return retriever
}

fun createMediaMetadataRetriever(fileDescriptor: FileDescriptor?): MediaMetadataRetriever? {
    return try {
        createMediaMetadataRetrieverOrThrow(fileDescriptor)
    } catch (e: RuntimeException) {
        logger.e("a RuntimeException occurred during createMediaMetadataRetriever()", e)
        null
    }
}

@Throws(RuntimeException::class)
fun createMediaMetadataRetrieverOrThrow(fileDescriptor: FileDescriptor?): MediaMetadataRetriever {
    val retriever: MediaMetadataRetriever
    if (fileDescriptor != null && fileDescriptor.valid()) {
        retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(fileDescriptor)
        } catch (e: RuntimeException) {
            throw RuntimeException("Cannot set data source $fileDescriptor", e)
        }
    } else {
        throw IllegalArgumentException("fileDescriptor is null or not valid: $fileDescriptor")
    }
    return retriever
}

fun extractMetadataFromFile(file: File?): MediaMetadata? {
    return if (file != null && isFileValid(file)) extractMetadataFromFile(file.absolutePath) else null
}

fun extractMetadataFromFile(filePath: String?): MediaMetadata? {
    return if (!isEmpty(filePath)) extractMetadata(Uri.parse(filePath), null) else null
}

fun extractMetadataFromFile(fileDescriptor: FileDescriptor?): MediaMetadata? {
    val retriever = createMediaMetadataRetriever(fileDescriptor) ?: return null
    return extractMetadata(retriever)
}


@JvmOverloads
fun extractMetadata(resourceUri: Uri?, headers: Map<String, String>? = null): MediaMetadata? {
    val retriever = createMediaMetadataRetriever(resourceUri, headers) ?: return null
    return extractMetadata(retriever)
}

fun extractMetadata(retriever: MediaMetadataRetriever): MediaMetadata {
    val metadata = MediaMetadata(
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_DURATION, Long::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_ALBUM, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_ARTIST, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_AUTHOR, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_COMPOSER, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_DATE, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_GENRE, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_YEAR, Int::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS, Int::class.java)
                    ?: 0,
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_WRITER, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_MIMETYPE, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_COMPILATION, String::class.java),
            extractMetadataField(retriever, 21, String::class.java),
            extractMetadataField(retriever, 22, Boolean::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO, Boolean::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO, Boolean::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH, Int::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT, Int::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_BITRATE, Int::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_LOCATION, String::class.java),
            extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION, Int::class.java),
            if (isAtLeastMarshmallow()) {
                extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE, Int::class.java)
            } else {
                null
            })
    retriever.release()
    return metadata
}

@JvmOverloads
fun <M> extractMetadataFieldFromFile(
        file: File?,
        keyCode: Int,
        headers: Map<String, String>? = null,
        clazz: Class<M>,
        defaultValue: M? = null
): M? {
    return if (file != null && isFileValid(file)) extractMetadataFieldFromFile(file.absolutePath, keyCode, headers, clazz, defaultValue) else null
}

@JvmOverloads
fun <M> extractMetadataFieldFromFile(
        filePath: String?,
        keyCode: Int,
        headers: Map<String, String>? = null,
        clazz: Class<M>,
        defaultValue: M? = null
): M? {
    return if (!isEmpty(filePath)) extractMetadataField(Uri.parse(filePath), keyCode, headers, clazz, defaultValue) else null
}

@JvmOverloads
fun <M> extractMetadataField(
        url: String?,
        keyCode: Int,
        headers: Map<String, String>? = null,
        clazz: Class<M>,
        defaultValue: M? = null
): M? {
    return if (!isEmpty(url)) extractMetadataField(Uri.parse(url), keyCode, headers, clazz, defaultValue) else null
}

@JvmOverloads
fun <M> extractMetadataField(
        resourceUri: Uri?,
        keyCode: Int,
        headers: Map<String, String>? = null,
        clazz: Class<M>,
        defaultValue: M? = null
): M? {
    val retriever = createMediaMetadataRetriever(resourceUri, headers) ?: return null
    return extractMetadataField(retriever, keyCode, clazz, defaultValue, true)
}

@Suppress("UNCHECKED_CAST")
@Throws(IllegalArgumentException::class)
@JvmOverloads
fun <M> extractMetadataField(
        retriever: MediaMetadataRetriever,
        keyCode: Int,
        clazz: Class<M>,
        defaultValue: M? = null,
        release: Boolean = false
): M? {
    val value = retriever.extractMetadata(keyCode) ?: EMPTY_STRING
    val isEmpty = isEmpty(value)
    return try {
        if (clazz.isAssignableFrom(String::class.java)) {
            if (!isEmpty) value as M else defaultValue
        } else if (clazz.isAssignableFrom(Long::class.java)) {
            value.toLongOrNull() as M? ?: defaultValue
        } else if (clazz.isAssignableFrom(Int::class.java)) {
            value.toIntOrNull() as M? ?: defaultValue
        } else if (clazz.isAssignableFrom(Boolean::class.java)) {
            if (!isEmpty) value.toBoolean() as M? else defaultValue
        } else {
            throw IllegalArgumentException("incorrect class: $clazz")
        }
    } catch (e: ClassCastException) {
        logger.e("value $value cannot be casted to $clazz", e)
        defaultValue
    } finally {
        if (release) {
            retriever.release()
        }
    }
}

/**
 * @param contentUri must have scheme "content://"
 */
fun extractAlbumArt(context: Context, contentUri: Uri?): Bitmap? {
    val albumId = queryUriFirst(context, contentUri, Long::class.java, listOf(MediaStore.Audio.Media.ALBUM_ID))
    if (albumId != null) {
        val coverUri = Uri.parse("content://media/external/audio/albumart")
        val trackCoverUri = ContentUris.withAppendedId(coverUri, albumId)
        return GraphicUtils.createBitmapFromUri(context, trackCoverUri, 1)
    }
    return null
}

@JvmOverloads
fun getMediaFileCoverArt(filePath: String?, headers: Map<String, String>? = null): Bitmap? {
    return if (!isEmpty(filePath)) getMediaFileCoverArt(Uri.Builder().scheme(ContentResolver.SCHEME_FILE).appendEncodedPath(filePath).build(), headers) else null
}

@JvmOverloads
fun getMediaFileCoverArt(resourceUri: Uri?, headers: Map<String, String>? = null): Bitmap? {
    val retriever = createMediaMetadataRetriever(resourceUri, headers)
    return if (retriever != null) GraphicUtils.createBitmapFromByteArray(retriever.embeddedPicture) else null
}

@JvmOverloads
fun extractMediaDurationFromFile(file: File?, headers: Map<String, String>? = null): Long? {
    return if (file != null && isFileValid(file)) extractMediaDurationFromFile(file.absolutePath, headers) else null
}

@JvmOverloads
fun extractMediaDurationFromFile(filePath: String?, headers: Map<String, String>? = null): Long? {
    return if (!isEmpty(filePath)) extractMediaDuration(Uri.parse(filePath), headers) else null
}

fun extractMediaDurationFromFile(fileDescriptor: FileDescriptor?): Long? {
    val retriever = createMediaMetadataRetriever(fileDescriptor) ?: return null
    return extractMediaDuration(retriever, true)
}

@JvmOverloads
fun extractMediaDuration(resourceUri: Uri?, headers: Map<String, String>? = null): Long? {
    val retriever = createMediaMetadataRetriever(resourceUri, headers) ?: return null
    return extractMediaDuration(retriever, true)
}

@JvmOverloads
fun extractMediaDuration(retriever: MediaMetadataRetriever, release: Boolean, defaultValue: Long? = null): Long? {
    return extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_DURATION, Long::class.java, defaultValue, release)
}

@JvmOverloads
fun extractFrameAtPositionFromFile(file: File?, positionMs: Long, headers: Map<String, String>? = null): Bitmap? {
    return if (file != null && isFileValid(file)) extractFrameAtPositionFromFile(file.absolutePath, positionMs, headers) else null
}

@JvmOverloads
fun extractFrameAtPositionFromFile(filePath: String?, positionMs: Long, headers: Map<String, String>? = null): Bitmap? {
    return if (!isEmpty(filePath)) extractFrameAtPosition(Uri.parse(filePath), positionMs, headers) else null
}

fun extractFrameAtPositionFromFile(fileDescriptor: FileDescriptor?, positionMs: Long): Bitmap? {
    val retriever = createMediaMetadataRetriever(fileDescriptor) ?: return null
    return extractFrameAtPosition(retriever, positionMs, true)
}

@JvmOverloads
fun extractFrameAtPosition(resourceUri: String?, positionMs: Long, headers: Map<String, String>? = null): Bitmap? {
    return if (!isEmpty(resourceUri)) extractFrameAtPosition(Uri.parse(resourceUri), positionMs, headers) else null
}

@JvmOverloads
fun extractFrameAtPosition(resourceUri: Uri?, positionMs: Long, headers: Map<String, String>? = null): Bitmap? {
    val retriever = createMediaMetadataRetriever(resourceUri, headers) ?: return null
    return extractFrameAtPosition(retriever, positionMs, true)
}

@JvmOverloads
fun extractFrameAtPosition(
        retriever: MediaMetadataRetriever,
        positionMs: Long,
        release: Boolean,
        option: Int = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
): Bitmap? {
    return try {
        extractMediaDuration(retriever, false)?.let {
            if (positionMs <= 0 || positionMs > it) {
                logger.e("Incorrect position: $positionMs")
                return null
            }
            retriever.getFrameAtTime(TimeUnit.MILLISECONDS.toNanos(positionMs), option)
        }
    } finally {
        if (release) {
            retriever.release()
        }
    }
}

@JvmOverloads
fun extractFramesFromFile(file: File?, framesCount: Int, headers: Map<String, String>? = null): Map<Long, Bitmap?> {
    return if (file != null && isFileValid(file)) extractFramesFromFile(file.absolutePath, framesCount, headers) else emptyMap()
}

@JvmOverloads
fun extractFramesFromFile(filePath: String?, framesCount: Int, headers: Map<String, String>? = null): Map<Long, Bitmap?> {
    return if (!isEmpty(filePath)) extractFrames(Uri.parse(filePath), framesCount, headers) else emptyMap()
}

fun extractFramesFromFile(fileDescriptor: FileDescriptor?, framesCount: Int): Map<Long, Bitmap?> {
    val retriever = createMediaMetadataRetriever(fileDescriptor) ?: return emptyMap()
    return extractFrames(retriever, framesCount, true)
}

@JvmOverloads
fun extractFrames(uri: String?, framesCount: Int, headers: Map<String, String>? = null): Map<Long, Bitmap?> {
    return if (!isEmpty(uri)) extractFrames(Uri.parse(uri), framesCount, headers) else emptyMap()
}

@JvmOverloads
fun extractFrames(resourceUri: Uri?, framesCount: Int, headers: Map<String, String>? = null): Map<Long, Bitmap?> {
    val retriever = createMediaMetadataRetriever(resourceUri, headers) ?: return emptyMap()
    return extractFrames(retriever, framesCount, true)
}

@JvmOverloads
fun extractFrames(
        retriever: MediaMetadataRetriever,
        framesCount: Int,
        release: Boolean,
        option: Int = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
): Map<Long, Bitmap?> {
    try {
        if (framesCount <= 0) {
            logger.e("Incorrect framesCount: $framesCount")
            return emptyMap()
        }
        val duration = extractMediaDuration(retriever, false) ?: 0
        if (duration <= 0) {
            logger.e("Incorrect duration: $duration")
            return emptyMap()
        }
        // logger.d("video durationMs: " + durationMs + " ms");
        val interval = duration / framesCount
        // logger.d("interval between frames: " + interval + " ms");
        var lastPosition: Long = 1
        val videoFrames = mutableMapOf<Long, Bitmap?>()
        while (lastPosition <= duration && videoFrames.size < framesCount) { // (durationMs - interval)
            // logger.d("getting frame at position: " + lastPosition + " ms");
            videoFrames[lastPosition] = extractFrameAtPosition(retriever, lastPosition, false, option)
            lastPosition += interval
            // logger.d("next position: " + lastPosition + " ms");
        }
        return videoFrames
    } finally {
        if (release) {
            retriever.release()
        }
    }
}

data class MediaMetadata(
        val durationMs: Long?,
        val cdTrackNumber: String?,
        val album: String?,
        val artist: String?,
        val author: String?,
        val composer: String?,
        val date: String?,
        val genre: String?,
        val title: String?,
        val year: Int?,
        val numTracks: Int?,
        val writer: String?,
        val mimeType: String?,
        val albumArtist: String?,
        val compilation: String?,
        val timedTextLanguages: String?,
        val isDrm: Boolean?,
        val hasAudio: Boolean?,
        val hasVideo: Boolean?,
        val videoWidth: Int?,
        val videoHeight: Int?,
        val bitrate: Int?,
        val location: String?,
        val videoRotation: Int?,
        val captureFrameRate: Int?
)
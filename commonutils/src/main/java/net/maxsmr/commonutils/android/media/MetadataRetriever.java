package net.maxsmr.commonutils.android.media;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.graphic.GraphicUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.os.Build.VERSION_CODES.M;

public final class MetadataRetriever {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(MetadataRetriever.class);

    private MetadataRetriever() {
        throw new UnsupportedOperationException("no instances.");
    }

    @NotNull
    public static MediaMetadataRetriever createMediaMetadataRetriever(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers) throws RuntimeException {

        MediaMetadataRetriever retriever;

        if (resourceUri != null && !resourceUri.toString().isEmpty()) {

            retriever = new MediaMetadataRetriever();

            try {
                if (TextUtils.isEmpty(resourceUri.getScheme()) || resourceUri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_FILE)) {
                    retriever.setDataSource(resourceUri.getPath());
                } else {
                    if (context == null && (headers == null)) {
                        throw new NullPointerException("scheme is not empty or " + ContentResolver.SCHEME_FILE + " and context was not specified");
                    }
                    if (headers == null) {
                        retriever.setDataSource(context, resourceUri);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            retriever.setDataSource(resourceUri.toString(), headers);
                        } else {
                            retriever.setDataSource(context, resourceUri);
                        }
                    }
                }
            } catch (RuntimeException e) {
                throw new RuntimeException("can't set data source", e);
            }
        } else {
            throw new IllegalArgumentException("resourceUri is empty");
        }

        return retriever;
    }

    @NotNull
    public static MediaMetadataRetriever createMediaMetadataRetriever(@Nullable FileDescriptor fileDescriptor) throws RuntimeException {

        MediaMetadataRetriever retriever;

        if (fileDescriptor != null && fileDescriptor.valid()) {

            retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(fileDescriptor);
            } catch (RuntimeException e) {
                throw new RuntimeException("can't set data source", e);
            }
        } else {
            throw new IllegalArgumentException("fileDescriptor is null or not valid: " + fileDescriptor);
        }

        return retriever;
    }

    @Nullable
    public static MediaMetadataRetriever createMediaMetadataRetrieverNoThrow(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers) {
        try {
            return createMediaMetadataRetriever(context, resourceUri, headers);
        } catch (RuntimeException e) {
            logger.e("a RuntimeException occurred during createMediaMetadataRetriever()", e);
            return null;
        }
    }

    @Nullable
    public static MediaMetadataRetriever createMediaMetadataRetrieverNoThrow(@Nullable FileDescriptor fileDescriptor) {
        try {
            return createMediaMetadataRetriever(fileDescriptor);
        } catch (RuntimeException e) {
            logger.e("a RuntimeException occurred during createMediaMetadataRetriever()", e);
            return null;
        }
    }

    @Nullable
    public static <M> M extractMetadataField(@NotNull Context context, @Nullable Uri resourceUri, int keyCode, @NotNull Class<M> clazz) {
        return extractMetadataField(context, resourceUri, keyCode, clazz, null);
    }

    @Nullable
    public static <M> M extractMetadataField(@NotNull Context context, @Nullable Uri resourceUri, int keyCode, @NotNull Class<M> clazz, @Nullable M defaultValue) {
        return extractMetadataField(context, resourceUri, null, keyCode, clazz, defaultValue);
    }

    @Nullable
    public static <M> M extractMetadataField(@Nullable File file, int keyCode, @NotNull Class<M> clazz) {
        return extractMetadataField(file, keyCode, clazz, null);
    }

    @Nullable
    public static <M> M extractMetadataField(@Nullable File file, int keyCode, @NotNull Class<M> clazz, @Nullable M defaultValue) {
        return FileHelper.isFileCorrect(file) ? extractMetadataField(file.getAbsolutePath(), keyCode, clazz, defaultValue) : null;
    }

    @Nullable
    public static <M> M extractMetadataField(@Nullable String filePath, int keyCode, @NotNull Class<M> clazz) {
        return extractMetadataField(filePath, keyCode, clazz, null);
    }

    @Nullable
    public static <M> M extractMetadataField(@Nullable String filePath, int keyCode, @NotNull Class<M> clazz, @Nullable M defaultValue) {
        return !TextUtils.isEmpty(filePath) ? extractMetadataField(null, Uri.parse(filePath), null, keyCode, clazz, defaultValue) : null;
    }

    @Nullable
    public static <M> M extractMetadataField(@Nullable String url, @Nullable Map<String, String> headers, int keyCode, @NotNull Class<M> clazz) {
        return extractMetadataField(url, headers, keyCode, clazz, null);
    }

    @Nullable
    public static <M> M extractMetadataField(@Nullable String url, @Nullable Map<String, String> headers, int keyCode, @NotNull Class<M> clazz, @Nullable M defaultValue) {
        return !TextUtils.isEmpty(url) ? extractMetadataField(null, Uri.parse(url), headers, keyCode, clazz, defaultValue) : null;
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static <M> M extractMetadataField(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers, int keyCode, @NotNull Class<M> clazz) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(context, resourceUri, headers);
        if (retriever == null) {
            return null;
        }
        try {
            return extractMetadataField(retriever, keyCode, clazz, null);
        } finally {
            retriever.release();
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static <M> M extractMetadataField(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers, int keyCode, @NotNull Class<M> clazz, @Nullable M defaultValue) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(context, resourceUri, headers);
        if (retriever == null) {
            return null;
        }
        return extractMetadataField(retriever, keyCode, clazz, defaultValue);
    }

    @NotNull
    public static MediaMetadata extractMetadata(@NotNull Context context, @Nullable Uri resourceUri) {
        return extractMetadata(context, resourceUri, null);
    }

    @NotNull
    public static MediaMetadata extractMetadata(@Nullable File file) {
        return FileHelper.isFileCorrect(file) ? extractMetadata(file.getAbsolutePath()) : new MediaMetadata();
    }

    @NotNull
    public static MediaMetadata extractMetadata(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) ? extractMetadata(null, Uri.parse(filePath), null) : new MediaMetadata();
    }

    @NotNull
    public static MediaMetadata extractMetadata(@Nullable String url, @Nullable Map<String, String> headers) {
        return !TextUtils.isEmpty(url) ? extractMetadata(null, Uri.parse(url), headers) : new MediaMetadata();
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public static MediaMetadata extractMetadata(@NotNull MediaMetadataRetriever retriever) {

        MediaMetadata metadata = new MediaMetadata();

        metadata.durationMs = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_DURATION, Integer.class);
        metadata.cdTrackNumber = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER, String.class);
        metadata.album = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_ALBUM, String.class);
        metadata.artist = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_ARTIST, String.class);
        metadata.author = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_AUTHOR, String.class);
        metadata.composer = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_COMPOSER, String.class);
        metadata.date = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_DATE, String.class);
        metadata.genre = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_GENRE, String.class);
        metadata.title = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE, String.class);
        metadata.year = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_YEAR, Integer.class);
        metadata.numTracks = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS, Integer.class);
        metadata.writer = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_WRITER, String.class);
        metadata.mimeType = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_MIMETYPE, String.class);
        metadata.albumArtist = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, String.class);
        metadata.compilation = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_COMPILATION, String.class);
        metadata.isDrm = extractMetadataField(retriever, 22, Boolean.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            metadata.hasAudio = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO, Boolean.class);
            metadata.hasVideo = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO, Boolean.class);
            metadata.videoSize = new Point(extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH, Integer.class), extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT, Integer.class));
            metadata.bitrate = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_BITRATE, Integer.class);
            metadata.timedTextLanguages = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_BITRATE, String.class);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            metadata.location = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_LOCATION, String.class);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            metadata.videoRotation = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION, Integer.class);
        }
        if (Build.VERSION.SDK_INT >= M) {
            metadata.captureFrameRate = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE, Integer.class);
        }

        retriever.release();
        return metadata;
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public static MediaMetadata extractMetadata(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers) {
        return extractMetadata(createMediaMetadataRetriever(context, resourceUri, headers));
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public static MediaMetadata extractMetadata(@Nullable FileDescriptor fileDescriptor) {
        return extractMetadata(createMediaMetadataRetriever(fileDescriptor));
    }

    @Nullable
    public static <M> M extractMetadataField(@NotNull MediaMetadataRetriever retriever, int keyCode, @NotNull Class<M> clazz) {
        return extractMetadataField(retriever, keyCode, clazz, null);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <M> M extractMetadataField(@NotNull MediaMetadataRetriever retriever, int keyCode, @NotNull Class<M> clazz, @Nullable M defaultValue) {
        final String value = retriever.extractMetadata(keyCode);
        final boolean isEmpty = TextUtils.isEmpty(value);
        try {
            if (clazz.isAssignableFrom(String.class)) {
                return !isEmpty ? (M) value : defaultValue;
            } else if (clazz.isAssignableFrom(Long.class)) {
                try {
                    return !isEmpty ? (M) Long.valueOf(value) : (defaultValue != null ? defaultValue : (M) Long.valueOf(0));
                } catch (NumberFormatException e) {
                    logger.e("a NumberFormatException occurred during valueOf(): " + e.getMessage(), e);
                    return defaultValue;
                }
            } else if (clazz.isAssignableFrom(Integer.class)) {
                try {
                    return !isEmpty ? (M) Integer.valueOf(value) : (defaultValue != null ? defaultValue : (M) Integer.valueOf(0));
                } catch (NumberFormatException e) {
                    logger.e("a NumberFormatException occurred during valueOf(): " + e.getMessage(), e);
                    return defaultValue;
                }
            } else if (clazz.isAssignableFrom(Boolean.class)) {
                return !isEmpty ? (M) Boolean.valueOf(value) : (defaultValue != null ? defaultValue : (M) Boolean.valueOf(false));
            } else {
                throw new UnsupportedOperationException("incorrect class: " + clazz);
            }
        } catch (ClassCastException e) {
            logger.e("value " + value + " cannot be casted to " + clazz, e);
            return defaultValue;
        }
    }

    /**
     * @param contentUri must have scheme "content://"
     */
    @Nullable
    public static Bitmap extractAlbumArt(@NotNull Context context, @Nullable Uri contentUri) {

        if (contentUri == null) {
            return null;
        }

        if (!ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(contentUri.getScheme())) {
            throw new IllegalArgumentException("incorrect uri scheme: " + contentUri.getScheme() + ", must be " + ContentResolver.SCHEME_CONTENT);
        }

        String[] projections = {MediaStore.Audio.Media.ALBUM_ID};

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(contentUri, projections, null, null, null);
            if (cursor != null && cursor.isClosed() && cursor.getCount() > 0) {
                Long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                cursor.moveToFirst();
                Uri coverUri = Uri.parse("content://media/external/audio/albumart");
                Uri trackCoverUri = ContentUris.withAppendedId(coverUri, albumId);
                return GraphicUtils.createBitmapFromUri(context, trackCoverUri, 1);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    @Nullable
    public static Bitmap getMediaFileCoverArt(@NotNull Context context, @Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) ? getMediaFileCoverArt(context, new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).appendEncodedPath(filePath).toString()) : null;
    }

    @Nullable
    public static Bitmap getMediaFileCoverArt(@NotNull Context context, @Nullable Uri resourceUri) {

        if (resourceUri == null) {
            return null;
        }

        if (!TextUtils.isEmpty(resourceUri.getScheme()) && !ContentResolver.SCHEME_FILE.equalsIgnoreCase(resourceUri.getScheme())) {
            throw new IllegalArgumentException("incorrect uri scheme: " + resourceUri.getScheme() + ", must be " + ContentResolver.SCHEME_FILE);
        }

        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(context, resourceUri, null);
        return retriever != null ? GraphicUtils.createBitmapFromByteArray(retriever.getEmbeddedPicture()) : null;
    }

    public static long extractMediaDuration(@NotNull Context context, @Nullable Uri resourceUri) {
        return extractMediaDuration(context, resourceUri, null);
    }

    public static long extractMediaDuration(@Nullable File file) {
        return FileHelper.isFileCorrect(file) ? extractMediaDuration(file.getAbsolutePath()) : 0;
    }

    public static long extractMediaDuration(@Nullable String filePath) {
        return !TextUtils.isEmpty(filePath) ? extractMediaDuration(null, Uri.parse(filePath), null) : 0;
    }

    public static long extractMediaDuration(@Nullable String uri, @Nullable Map<String, String> headers) {
        return !TextUtils.isEmpty(uri) ? extractMediaDuration(null, Uri.parse(uri), headers) : 0;
    }

    public static long extractMediaDuration(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(context, resourceUri, headers);
        return retriever != null ? extractMediaDuration(retriever, true) : 0;
    }

    public static long extractMediaDuration(@Nullable FileDescriptor fileDescriptor) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(fileDescriptor);
        return retriever != null ? extractMediaDuration(retriever, true) : 0;
    }

    public static long extractMediaDuration(@NotNull MediaMetadataRetriever retriever, boolean release) {
        Long duration = extractMetadataField(retriever, MediaMetadataRetriever.METADATA_KEY_DURATION, Long.class);
        try {
            return duration != null ? duration : 0;
        } finally {
            if (release) {
                retriever.release();
            }
        }
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@NotNull Context context, @Nullable Uri resourceUri, long positionMs) {
        return extractFrameAtPosition(context, resourceUri, null, positionMs);
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@Nullable File file, long positionMs) {
        return FileHelper.isFileCorrect(file) ? extractFrameAtPosition(file.getAbsolutePath(), positionMs) : null;
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@Nullable String filePath, long positionMs) {
        return !TextUtils.isEmpty(filePath) ? extractFrameAtPosition(null, Uri.parse(filePath), null, positionMs) : null;
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@Nullable String uri, @Nullable Map<String, String> headers, long positionMs) {
        return !TextUtils.isEmpty(uri) ? extractFrameAtPosition(null, Uri.parse(uri), headers, positionMs) : null;
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers, long positionMs) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(context, resourceUri, headers);
        return retriever != null ? extractFrameAtPosition(retriever, positionMs, true) : null;
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@Nullable FileDescriptor fileDescriptor, long positionMs) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(fileDescriptor);
        return retriever != null ? extractFrameAtPosition(retriever, positionMs, true) : null;
    }

    @Nullable
    public static Bitmap extractFrameAtPosition(@NotNull MediaMetadataRetriever retriever, long positionMs, boolean release) {
        try {
            if (positionMs <= 0 || positionMs > extractMediaDuration(retriever, false)) {
                logger.e("incorrect position: " + positionMs);
                return null;
            }
            return retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } finally {
            if (release) {
                retriever.release();
            }
        }
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@NotNull Context context, @Nullable Uri resourceUri, int framesCount) {
        return extractFrames(context, resourceUri, null, framesCount);
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@Nullable File file, int framesCount) {
        return FileHelper.isFileCorrect(file) ? extractFrames(file.getAbsolutePath(), framesCount) : Collections.<Long, Bitmap>emptyMap();
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@Nullable String filePath, int framesCount) {
        return !TextUtils.isEmpty(filePath) ? extractFrames(null, Uri.parse(filePath), null, framesCount) : Collections.<Long, Bitmap>emptyMap();
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@Nullable String uri, @Nullable Map<String, String> headers, int framesCount) {
        return !TextUtils.isEmpty(uri) ? extractFrames(null, Uri.parse(uri), headers, framesCount) : Collections.<Long, Bitmap>emptyMap();
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@Nullable Context context, @Nullable Uri resourceUri, @Nullable Map<String, String> headers, int framesCount) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(context, resourceUri, headers);
        return retriever != null ? extractFrames(retriever, framesCount, true) : Collections.<Long, Bitmap>emptyMap();
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@Nullable FileDescriptor fileDescriptor, int framesCount) {
        MediaMetadataRetriever retriever = createMediaMetadataRetrieverNoThrow(fileDescriptor);
        return retriever != null ? extractFrames(retriever, framesCount, true) : Collections.<Long, Bitmap>emptyMap();
    }

    @NotNull
    public static Map<Long, Bitmap> extractFrames(@NotNull MediaMetadataRetriever retriever, int framesCount, boolean release) {

        try {

            if (framesCount <= 0) {
                logger.e("incorrect framesCount: " + framesCount);
                return Collections.emptyMap();
            }

            final long duration = extractMediaDuration(retriever, false);
            // logger.d("video durationMs: " + durationMs + " ms");

            final long interval = duration / framesCount;
            // logger.d("interval between frames: " + interval + " ms");
            long lastPosition = 1;

            Map<Long, Bitmap> videoFrames = new LinkedHashMap<>(framesCount);

            while (lastPosition <= duration && videoFrames.size() < framesCount) { // (durationMs - interval)
                // logger.d("getting frame at position: " + lastPosition + " ms");
                videoFrames.put(lastPosition, extractFrameAtPosition(retriever, lastPosition, false));
                lastPosition += interval;
                // logger.d("next position: " + lastPosition + " ms");
            }

            return videoFrames;

        } finally {
            if (release) {
                retriever.release();
            }
        }
    }

    public static class MediaMetadata {

        public long durationMs;

        public String cdTrackNumber;

        public String album;

        public String artist;

        public String author;

        public String composer;

        public String date;

        public String genre;

        public String title;

        public int year;

        public int numTracks;

        public String writer;

        public String mimeType;

        public String albumArtist;

        public String compilation;

        public boolean hasAudio;

        public boolean hasVideo;

        public Point videoSize;

        public int bitrate;

        public String timedTextLanguages;

        public boolean isDrm;

        public String location;

        public int videoRotation;

        public int captureFrameRate;

        @Override
        public String toString() {
            return "MediaMetadata{" +
                    "durationMs=" + durationMs +
                    ", cdTrackNumber=" + cdTrackNumber +
                    ", album='" + album + '\'' +
                    ", artist='" + artist + '\'' +
                    ", author='" + author + '\'' +
                    ", composer='" + composer + '\'' +
                    ", date='" + date + '\'' +
                    ", genre='" + genre + '\'' +
                    ", title='" + title + '\'' +
                    ", year=" + year +
                    ", numTracks=" + numTracks +
                    ", writer='" + writer + '\'' +
                    ", mimeType='" + mimeType + '\'' +
                    ", albumArtist='" + albumArtist + '\'' +
                    ", compilation='" + compilation + '\'' +
                    ", hasAudio=" + hasAudio +
                    ", hasVideo=" + hasVideo +
                    ", videoSize=" + videoSize +
                    ", bitrate=" + bitrate +
                    ", timedTextLanguages='" + timedTextLanguages + '\'' +
                    ", isDrm=" + isDrm +
                    ", location='" + location + '\'' +
                    ", videoRotation=" + videoRotation +
                    ", captureFrameRate=" + captureFrameRate +
                    '}';
        }
    }
}
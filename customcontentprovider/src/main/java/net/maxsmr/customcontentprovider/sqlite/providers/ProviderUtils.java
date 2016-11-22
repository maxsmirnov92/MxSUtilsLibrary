package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ProviderUtils {

    private final static Logger logger = LoggerFactory.getLogger(ProviderUtils.class);

    private ProviderUtils() {
        throw new AssertionError("no instances.");
    }

    public static boolean isCorrectUri(@Nullable Uri uri) {
        return uri != null && !TextUtils.isEmpty(uri.getScheme()) && uri.getScheme().equalsIgnoreCase(AbstractSQLiteContentProvider.SCHEME_CONTENT_PROVIDER);
    }

    public static <P extends ContentProvider> ProviderInfo getProviderInfo(@NonNull Context context, @Nullable String packageName, @NonNull Class<P> providerClass, int flags)
            throws PackageManager.NameNotFoundException {

        return context.getPackageManager().getProviderInfo(new ComponentName(packageName, providerClass.getName()), flags);
    }

    /**
     * @return authorities associated with this ContentProvider (defined in AndroidManifest.xml)
     */
    @Nullable
    public static <P extends ContentProvider> String[] getAuthorities(@NonNull Context context, String packageName, @NonNull Class<P> providerClass) {

        final ProviderInfo pi;

        try {
            pi = getProviderInfo(context, packageName, providerClass, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            logger.error("a NameNotFoundException occurred during getProviderInfo()", e);
            return null;
        }

        return TextUtils.split(pi.authority, ";");
    }

    @Nullable
    public static <P extends ContentProvider> Uri getContentProviderUri(@NonNull Context context, @Nullable String packageName, @NonNull Class<P> providerClass) {
        ProviderInfo providerInfo;
        try {
            providerInfo = getProviderInfo(context, packageName, providerClass, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            logger.error("a NameNotFoundException occurred during getProviderInfo()", e);
            return null;
        }
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(AbstractSQLiteContentProvider.SCHEME_CONTENT_PROVIDER);
        uriBuilder.encodedAuthority(providerInfo.authority);
        return uriBuilder.build();
    }

    @Nullable
    public static <P extends ContentProvider> Uri getContentProviderTableUri(@NonNull Context context, @Nullable String packageName, @NonNull Class<P> providerClass, @NonNull String tableName) {
        Uri contentProviderUri = getContentProviderUri(context, packageName, providerClass);
        if (contentProviderUri != null) {
            Uri.Builder uriBuilder = contentProviderUri.buildUpon();
            uriBuilder.appendEncodedPath(tableName);
            return uriBuilder.build();
        }
        return null;
    }

    @Nullable
    public static String getTableName(Uri uri) {
        if (isCorrectUri(uri)) {
            return uri.getPathSegments().get(0);
        }
        return null;
    }

    @NonNull
    public static List<String> getAllExistingColumns(Uri uri, Context ctx) {
        if (isCorrectUri(uri)) {
            Cursor c = ctx.getContentResolver().query(uri, null, null, null, BaseColumns._ID
                    + " ASC");
            if (c != null) {
                try {
                    return new ArrayList<>(Arrays.asList(c.getColumnNames()));
                } finally {
                    c.close();
                }
            }
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static <V> V getDataFromCursor(@Nullable Cursor c, @Nullable String columnName, @NonNull Class<V> dataClass) {
        if (c != null && !c.isClosed() && c.getCount() > 0) {
            if (!TextUtils.isEmpty(columnName)) {
                columnName = columnName.toLowerCase(Locale.getDefault());
                if (c.getPosition() == -1) {
                    c.moveToFirst();
                }
                try {
                    if (dataClass.isAssignableFrom(String.class)) {
                        return (V) c.getString(c.getColumnIndex(columnName));
                    } else if (dataClass.isAssignableFrom(Integer.class)) {
                        return (V) Integer.valueOf(c.getInt(c.getColumnIndex(columnName)));
                    } else if (dataClass.isAssignableFrom(Long.class)) {
                        return (V) Long.valueOf(c.getLong(c.getColumnIndex(columnName)));
                    } else if (dataClass.isAssignableFrom(Float.class)) {
                        return (V) Float.valueOf(c.getFloat(c.getColumnIndex(columnName)));
                    } else if (dataClass.isAssignableFrom(byte[].class)) {
                        return (V) c.getBlob(c.getColumnIndex(columnName));
                    } else {
                        throw new UnsupportedOperationException("incorrect data class: " + dataClass);
                    }
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    logger.error("a ClassCastException occurred", e);
                }
            }
        }
        return null;
    }
}

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

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ProviderUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ProviderUtils.class);

    private ProviderUtils() {
        throw new AssertionError("no instances.");
    }

    public static boolean isCorrectUri(@Nullable Uri uri) {
        return uri != null && !TextUtils.isEmpty(uri.getScheme()) && uri.getScheme().equalsIgnoreCase(AbstractSQLiteContentProvider.SCHEME_CONTENT_PROVIDER);
    }

    public static <P extends ContentProvider> ProviderInfo getProviderInfo(@NonNull Context context, @Nullable String packageName, @NonNull Class<P> providerClass) {
        return getProviderInfo(context, packageName,  providerClass,0);
    }

    public static <P extends ContentProvider> ProviderInfo getProviderInfo(@NonNull Context context, @Nullable String packageName, @NonNull Class<P> providerClass, int flags) {
        if (!TextUtils.isEmpty(packageName)) {
            try {
                return context.getPackageManager().getProviderInfo(new ComponentName(packageName, providerClass.getName()), flags);
            } catch (PackageManager.NameNotFoundException e) {
                logger.e("a NameNotFoundException occurred during getProviderInfo(): " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * @return authorities associated with this ContentProvider (defined in AndroidManifest.xml)
     */
    @NonNull
    public static <P extends ContentProvider> Set<String> getAuthorities(@NonNull Context context, String packageName, @NonNull Class<P> providerClass) {
        final ProviderInfo pi = getProviderInfo(context, packageName, providerClass, PackageManager.GET_META_DATA);
        return pi != null? new LinkedHashSet<>(Arrays.asList(TextUtils.split(pi.authority, ";"))) : Collections.<String>emptySet();
    }

    @Nullable
    public static <P extends ContentProvider> Uri getContentProviderUri(@NonNull Context context, @Nullable String packageName, @NonNull Class<P> providerClass) {
        ProviderInfo providerInfo = getProviderInfo(context, packageName, providerClass, PackageManager.GET_META_DATA);
        if (providerInfo != null) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.scheme(AbstractSQLiteContentProvider.SCHEME_CONTENT_PROVIDER);
            uriBuilder.encodedAuthority(providerInfo.authority);
            return uriBuilder.build();
        }
        return null;
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
                    logger.e("a ClassCastException occurred: " + e.getMessage(), e);
                }
            }
        }
        return null;
    }
}

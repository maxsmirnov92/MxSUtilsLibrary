package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;


import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;

public final class ProviderUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ProviderUtils.class);

    private ProviderUtils() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    public static <P extends ContentProvider> ProviderInfo getProviderInfo(@NotNull Context context, @Nullable String packageName, @NotNull Class<P> providerClass) {
        return getProviderInfo(context, packageName,  providerClass,0);
    }

    @Nullable
    public static <P extends ContentProvider> ProviderInfo getProviderInfo(@NotNull Context context, @Nullable String packageName, @NotNull Class<P> providerClass, int flags) {
        if (!isEmpty(packageName)) {
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
    @NotNull
    public static <P extends ContentProvider> Set<String> getAuthorities(@NotNull Context context, String packageName, @NotNull Class<P> providerClass) {
        final ProviderInfo pi = getProviderInfo(context, packageName, providerClass, PackageManager.GET_META_DATA);
        return pi != null? new LinkedHashSet<>(Arrays.asList(TextUtils.split(pi.authority, ";"))) : Collections.<String>emptySet();
    }

    @Nullable
    public static <P extends ContentProvider> Uri getContentProviderUri(@NotNull Context context, @Nullable String packageName, @NotNull Class<P> providerClass) {
        ProviderInfo providerInfo = getProviderInfo(context, packageName, providerClass, PackageManager.GET_META_DATA);
        if (providerInfo != null) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.scheme(ContentResolver.SCHEME_CONTENT);
            uriBuilder.encodedAuthority(providerInfo.authority);
            return uriBuilder.build();
        }
        return null;
    }

    @Nullable
    public static <P extends ContentProvider> Uri getContentProviderTableUri(@NotNull Context context, @Nullable String packageName, @NotNull Class<P> providerClass, @NotNull String tableName) {
        Uri contentProviderUri = getContentProviderUri(context, packageName, providerClass);
        if (contentProviderUri != null) {
            Uri.Builder uriBuilder = contentProviderUri.buildUpon();
            uriBuilder.appendEncodedPath(tableName);
            return uriBuilder.build();
        }
        return null;
    }
}

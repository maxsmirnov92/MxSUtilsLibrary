package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.customcontentprovider.sqlite.ISQLiteOperation;
import net.maxsmr.customcontentprovider.sqlite.ISQLiteOperation.SQLiteOperation;
import net.maxsmr.customcontentprovider.sqlite.SQLiteUriMatcher;
import net.maxsmr.customcontentprovider.sqlite.SQLiteUriMatcher.URI_MATCH;
import net.maxsmr.customcontentprovider.sqlite.SQLiteUriMatcher.UriMatcherPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractSQLiteContentProvider extends ContentProvider {

    private final static Logger logger = LoggerFactory.getLogger(AbstractSQLiteContentProvider.class);

    public static final String SCHEME_CONTENT_PROVIDER = ContentResolver.SCHEME_CONTENT;

    public static final String MIME_DIR = "vnd.android.cursor.dir/";

    public static final String MIME_ITEM = "vnd.android.cursor.item/";

    private SQLiteOpenHelper sqLiteHelper;

    private final String databaseName;

    private final String databasePath;

    private final int databaseVersion;

    /**
     * @param databasePath if null - default location will be used, otherwise - path on sdcard (for e.g.
     *                     Android/data/com.example.database)
     */
    protected AbstractSQLiteContentProvider(String databaseName, String databasePath, int databaseVer, Set<? extends AbstractSQLiteTableProvider> tableProviders) {

        this.databaseName = databaseName;
        this.databasePath = databasePath;
        this.databaseVersion = databaseVer;
        this.tableProviders.addAll(tableProviders);

        checkFields();
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public int getDatabaseVersion() {
        return databaseVersion;
    }

    private final Set<AbstractSQLiteTableProvider> tableProviders = new LinkedHashSet<>();

    protected final void checkFields() throws RuntimeException {

        if (TextUtils.isEmpty(databaseName))
            throw new RuntimeException("databaseName is empty");

        if (!TextUtils.isEmpty(databasePath)) {
            if (!FileHelper.checkDirNoThrow(databasePath))
                throw new RuntimeException("can't create database directory: " + databasePath);
        }

        if (databaseVersion < 1)
            throw new RuntimeException("incorrect databaseVersion: " + databaseVersion);

        if (tableProviders == null || tableProviders.isEmpty())
            throw new RuntimeException("tableProviders is null or empty");

    }

    protected final boolean checkFieldsNoThrow() {
        try {
            checkFields();
            return true;
        } catch (RuntimeException e) {
            logger.error("a RuntimeException occurred during checkFields()", e);
            return false;
        }
    }

    private SQLiteUriMatcher uriMatcher;

    private List<UriMatcherPair> makeUriMatcherPairs() throws RuntimeException {

        final String[] authorities = getAuthorities();

        if (authorities == null || authorities.length == 0)
            throw new RuntimeException("no authorities for this ContentProvider : " + getClass());

        if (tableProviders == null || tableProviders.isEmpty())
            throw new RuntimeException("no tableProviders specified");

        List<UriMatcherPair> uriMatcherPairs = new ArrayList<>();

        for (String authority : authorities) {

            if (StringUtils.isEmpty(authority))
                continue;

            for (AbstractSQLiteTableProvider provider : tableProviders) {

                if (provider == null)
                    continue;

                uriMatcherPairs.add(new UriMatcherPair(authority, provider.getTableName()));
            }
        }

        if (uriMatcherPairs.isEmpty())
            throw new RuntimeException("no correct authorities / table names");

        return uriMatcherPairs;
    }

    public final String[] getAuthorities() {
        Context context = getContext();
        if (context == null) {
            throw new RuntimeException("context is null");
        }
        return ProviderUtils.getAuthorities(context, context.getPackageName(), getClass());
    }

    @Override
    public final String getType(@NonNull Uri uri) {

        if (uriMatcher == null)
            throw new RuntimeException("uriMatcher is null");

        final URI_MATCH matchResult = uriMatcher.match(uri);

        if (matchResult == null)
            throw new SQLiteException("Unknown URI: " + uri);

        switch (matchResult) {
            case NO_MATCH:
                throw new SQLiteException("Unknown URI: " + uri);
            case MATCH_ALL:
                return MIME_DIR + ProviderUtils.getTableName(uri);
            case MATCH_ID:
                return MIME_ITEM + ProviderUtils.getTableName(uri);
            default:
                throw new SQLiteException("Unknown URI: " + uri);
        }
    }

    @Override
    public final boolean onCreate() {
        logger.debug("onCreate()");

        if (getContext() == null) {
            throw new RuntimeException("context is null");
        }

        checkFields();

        uriMatcher = new SQLiteUriMatcher(makeUriMatcherPairs());
        sqLiteHelper = SQLiteOpenHelperImpl.createFrom(getContext(), databaseName, databasePath, databaseVersion, tableProviders);
        return true;
    }

    @Override
    public final Cursor query(@NonNull Uri uri, String[] columns, String where, String[] whereArgs, String orderBy) {
        logger.debug("query(), uri=" + uri + ", columns=" + Arrays.toString(columns) + ", where=" + where + ", whereArgs=" + Arrays.toString(whereArgs) + ", orderBy=" + orderBy);

        if (getContext() == null) {
            throw new RuntimeException("context is null");
        }

        final URI_MATCH matchResult = uriMatcher.match(uri);

        if (matchResult == URI_MATCH.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(tableProviders, tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == URI_MATCH.MATCH_ALL) {

            if (TextUtils.isEmpty(orderBy))
                orderBy = BaseColumns._ID + " ASC";

        } else if (matchResult == URI_MATCH.MATCH_ID) {

            if (TextUtils.isEmpty(where)) {
                where = BaseColumns._ID + "=?";
            } else {
                where += " AND " + BaseColumns._ID + "=?";
            }

            if (whereArgs == null || whereArgs.length == 0) {
                whereArgs = new String[]{uri.getLastPathSegment()};
            } else {

                String[] oldWhereArgs = whereArgs;
                whereArgs = new String[oldWhereArgs.length + 1];
                System.arraycopy(oldWhereArgs, 0, whereArgs, 0, oldWhereArgs.length);
                whereArgs[oldWhereArgs.length] = uri.getLastPathSegment();
            }

        }

        final Cursor cursor = tableProvider.query(sqLiteHelper.getReadableDatabase(), columns, where, whereArgs, orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public final Uri insert(@NonNull Uri uri, ContentValues values) {
        logger.debug("insert(), uri=" + uri + ", values=" + values);

        if (getContext() == null) {
            throw new RuntimeException("context is null");
        }

        final URI_MATCH matchResult = uriMatcher.match(uri);

        if (matchResult == URI_MATCH.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(tableProviders, tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == URI_MATCH.MATCH_ID) {

            final int affectedRows = updateInternal(tableProvider.getBaseUri(getContext()), tableProvider, values, BaseColumns._ID + "=?",
                    new String[]{uri.getLastPathSegment()});
            if (affectedRows > 0) {
                return uri;
            }
        }

        final long lastId = tableProvider.insert(sqLiteHelper.getWritableDatabase(), values);
        getContext().getContentResolver().notifyChange(tableProvider.getBaseUri(getContext()), null);

        final Bundle extras = new Bundle();
        extras.putLong(ISQLiteOperation.KEY_LAST_ID, lastId);
        tableProvider.onContentChanged(getContext(), SQLiteOperation.INSERT, extras);

        return uri;
    }

    @Override
    public final int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        logger.debug("delete(), uri=" + uri + ", where=" + where + ", whereArgs=" + Arrays.toString(whereArgs));

        if (getContext() == null) {
            throw new RuntimeException("context is null");
        }

        final URI_MATCH matchResult = uriMatcher.match(uri);

        if (matchResult == URI_MATCH.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(tableProviders, tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == URI_MATCH.MATCH_ID) {

            if (TextUtils.isEmpty(where)) {
                where = BaseColumns._ID + "=?";
            } else {
                where += " AND " + BaseColumns._ID + "=?";
            }

            if (whereArgs == null || whereArgs.length == 0) {
                whereArgs = new String[]{uri.getLastPathSegment()};
            } else {

                String[] oldWhereArgs = whereArgs;
                whereArgs = new String[oldWhereArgs.length + 1];
                System.arraycopy(oldWhereArgs, 0, whereArgs, 0, oldWhereArgs.length);
                whereArgs[oldWhereArgs.length] = uri.getLastPathSegment();
            }
        }

        final int affectedRows = tableProvider.delete(sqLiteHelper.getWritableDatabase(), where, whereArgs);

        if (affectedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);

            final Bundle extras = new Bundle();
            extras.putLong(ISQLiteOperation.KEY_AFFECTED_ROWS, affectedRows);
            tableProvider.onContentChanged(getContext(), SQLiteOperation.DELETE, extras);
        }
        return affectedRows;
    }

    @Override
    public final int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        logger.debug("update(), uri=" + uri + ", values=" + values + ", where=" + where + ", whereArgs=" + Arrays.toString(whereArgs));

        if (getContext() == null) {
            throw new RuntimeException("context is null");
        }

        final URI_MATCH matchResult = uriMatcher.match(uri);

        if (matchResult == URI_MATCH.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(tableProviders, tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == URI_MATCH.MATCH_ID) {

            if (TextUtils.isEmpty(where)) {
                where = BaseColumns._ID + "=?";
            } else {
                where += " AND " + BaseColumns._ID + "=?";
            }

            if (whereArgs == null || whereArgs.length == 0) {
                whereArgs = new String[]{uri.getLastPathSegment()};
            } else {

                String[] oldWhereArgs = whereArgs;
                whereArgs = new String[oldWhereArgs.length + 1];
                System.arraycopy(oldWhereArgs, 0, whereArgs, 0, oldWhereArgs.length);
                whereArgs[oldWhereArgs.length] = uri.getLastPathSegment();
            }
        }

        return updateInternal(tableProvider.getBaseUri(getContext()), tableProvider, values, where, whereArgs);
    }

    private int updateInternal(Uri uri, AbstractSQLiteTableProvider provider, ContentValues values, String where, String[] whereArgs) {

        if (getContext() == null) {
            throw new RuntimeException("context is null");
        }

        final int affectedRows = provider.update(sqLiteHelper.getWritableDatabase(), values, where, whereArgs);
        if (affectedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            final Bundle extras = new Bundle();
            extras.putLong(ISQLiteOperation.KEY_AFFECTED_ROWS, affectedRows);
            provider.onContentChanged(getContext(), SQLiteOperation.UPDATE, extras);
        }
        return affectedRows;
    }


}
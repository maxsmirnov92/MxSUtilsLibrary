package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.customcontentprovider.sqlite.ISQLiteOperation;
import net.maxsmr.customcontentprovider.sqlite.ISQLiteOperation.OperationType;
import net.maxsmr.customcontentprovider.sqlite.SQLiteUriMatcher;
import net.maxsmr.customcontentprovider.sqlite.SQLiteUriMatcher.UriMatch;
import net.maxsmr.customcontentprovider.sqlite.SQLiteUriMatcher.UriMatcherPair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.maxsmr.customcontentprovider.sqlite.providers.AbstractSQLiteTableProvider.Order.ASC;

public abstract class AbstractSQLiteContentProvider extends ContentProvider {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(AbstractSQLiteContentProvider.class);

    public static final String MIME_DIR = "vnd.android.cursor.dir/";

    public static final String MIME_ITEM = "vnd.android.cursor.item/";

    @NotNull
    private final ITableProvidersProvider tablesProvider;

    private final String databaseName;

    private final String databasePath;

    private final int databaseVersion;

    private SQLiteOpenHelperImpl sqLiteHelper;

    private SQLiteUriMatcher uriMatcher;

    /**
     * @param databasePath if null - default location will be used, otherwise - path on sdcard (for e.g.
     *                     Android/data/com.example.database)
     */
    protected AbstractSQLiteContentProvider(String databaseName, String databasePath, int databaseVer, @NotNull ITableProvidersProvider tablesProvider) {
        this.databaseName = databaseName;
        this.databasePath = databasePath;
        this.databaseVersion = databaseVer;
        this.tablesProvider = tablesProvider;
        checkFields();
    }

    @Override
    public final String getType(@NotNull Uri uri) {

        if (uriMatcher == null)
            throw new RuntimeException("uriMatcher is null");

        final UriMatch matchResult = uriMatcher.match(uri);

        if (matchResult == null)
            throw new SQLiteException("Unknown URI: " + uri);

        switch (matchResult) {
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
        logger.d("onCreate()");

        final Context context = getContext();

        if (context == null) {
            throw new RuntimeException("context is null");
        }

        checkFields();

        uriMatcher = new SQLiteUriMatcher(makeUriMatcherPairs());
        sqLiteHelper = SQLiteOpenHelperImpl.createFrom(context, databaseName, databasePath, databaseVersion, tablesProvider.provide());
        return true;
    }

    @Override
    public final Cursor query(@NotNull Uri uri, String[] columns, String where, String[] whereArgs, String orderBy) {
        logger.d("query(), uri=" + uri + ", columns=" + Arrays.toString(columns) + ", where=" + where + ", whereArgs=" + Arrays.toString(whereArgs) + ", orderBy=" + orderBy);

        final Context context = getContext();

        if (context == null) {
            throw new RuntimeException("context is null");
        }

        final UriMatch matchResult = uriMatcher.match(uri);

        if (matchResult == UriMatch.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(sqLiteHelper.getTableProviders(), tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == UriMatch.MATCH_ALL) {

            if (TextUtils.isEmpty(orderBy))
                orderBy = BaseColumns._ID + " " + ASC;

        } else if (matchResult == UriMatch.MATCH_ID) {

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
        cursor.setNotificationUri(context.getContentResolver(), uri);
        return cursor;
    }

    @Override
    public final Uri insert(@NotNull Uri uri, ContentValues values) {
        logger.d("insert(), uri=" + uri + ", values=" + values);

        final Context context = getContext();

        if (context == null) {
            throw new RuntimeException("context is null");
        }

        final UriMatch matchResult = uriMatcher.match(uri);

        if (matchResult == UriMatch.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(sqLiteHelper.getTableProviders(), tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        final Uri baseUri = tableProvider.getBaseUri(context);

        if (matchResult == UriMatch.MATCH_ID) {

            final int affectedRows = updateInternal(baseUri, tableProvider, values, BaseColumns._ID + "=?",
                    new String[]{uri.getLastPathSegment()});
            if (affectedRows > 0) {
                return uri;
            }
        }

        final long lastId = tableProvider.insert(sqLiteHelper.getWritableDatabase(), values);
        context.getContentResolver().notifyChange(baseUri, null);

        final Bundle extras = new Bundle();
        extras.putLong(ISQLiteOperation.KEY_LAST_ID, lastId);
        tableProvider.onContentChanged(context, OperationType.INSERT, extras);

        return uri;
    }

    @Override
    public final int delete(@NotNull Uri uri, String where, String[] whereArgs) {
        logger.d("delete(), uri=" + uri + ", where=" + where + ", whereArgs=" + Arrays.toString(whereArgs));

        final Context context = getContext();

        if (context == null) {
            throw new RuntimeException("context is null");
        }

        final UriMatch matchResult = uriMatcher.match(uri);

        if (matchResult == UriMatch.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(sqLiteHelper.getTableProviders(), tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == UriMatch.MATCH_ID) {

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
            context.getContentResolver().notifyChange(uri, null);

            final Bundle extras = new Bundle();
            extras.putLong(ISQLiteOperation.KEY_AFFECTED_ROWS, affectedRows);
            tableProvider.onContentChanged(context, OperationType.DELETE, extras);
        }
        return affectedRows;
    }

    @Override
    public final int update(@NotNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        logger.d("update(), uri=" + uri + ", values=" + values + ", where=" + where + ", whereArgs=" + Arrays.toString(whereArgs));

        final Context context = getContext();

        if (context == null) {
            throw new RuntimeException("context is null");
        }

        final UriMatch matchResult = uriMatcher.match(uri);

        if (matchResult == UriMatch.NO_MATCH)
            throw new SQLiteException("Unknown URI: " + uri);

        final String tableName = ProviderUtils.getTableName(uri);
        final AbstractSQLiteTableProvider tableProvider = AbstractSQLiteTableProvider.findSQLiteTableProvider(sqLiteHelper.getTableProviders(), tableName);

        if (tableProvider == null)
            throw new SQLiteException("No such table " + tableName + " specified in schema");

        if (matchResult == UriMatch.MATCH_ID) {

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

        return updateInternal(tableProvider.getBaseUri(context), tableProvider, values, where, whereArgs);
    }

    @NotNull
    public final Set<String> getAuthorities() {
        Context context = getContext();
        if (context == null) {
            throw new RuntimeException("context is null");
        }
        return ProviderUtils.getAuthorities(context, context.getPackageName(), getClass());
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

    protected final boolean checkFieldsNoThrow() {
        try {
            checkFields();
            return true;
        } catch (RuntimeException e) {
            logger.e("a RuntimeException occurred during checkFields()", e);
            return false;
        }
    }

    protected void checkFields() throws RuntimeException {

        if (TextUtils.isEmpty(databaseName))
            throw new RuntimeException("databaseName is empty");

        FileHelper.checkDir(databasePath);

        if (databaseVersion < 1)
            throw new RuntimeException("incorrect databaseVersion: " + databaseVersion);

    }

    @NotNull
    private List<UriMatcherPair> makeUriMatcherPairs() throws RuntimeException {

        final Set<String> authorities = getAuthorities();

        if (authorities.isEmpty())
            throw new RuntimeException("no authorities for this ContentProvider : " + getClass());

        final Set<AbstractSQLiteTableProvider> tableProviders = sqLiteHelper.getTableProviders();

        if (tableProviders.isEmpty())
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

    private int updateInternal(Uri uri, AbstractSQLiteTableProvider tableProvider, ContentValues values, String where, String[] whereArgs) {

        final Context context = getContext();

        if (context == null) {
            throw new RuntimeException("context is null");
        }

        final int affectedRows = tableProvider.update(sqLiteHelper.getWritableDatabase(), values, where, whereArgs);
        if (affectedRows > 0) {
            context.getContentResolver().notifyChange(uri, null);
            final Bundle extras = new Bundle();
            extras.putLong(ISQLiteOperation.KEY_AFFECTED_ROWS, affectedRows);
            tableProvider.onContentChanged(context, OperationType.UPDATE, extras);
        }
        return affectedRows;
    }

    public interface ITableProvidersProvider {

        @NotNull
        Set<? extends AbstractSQLiteTableProvider> provide();
    }
}
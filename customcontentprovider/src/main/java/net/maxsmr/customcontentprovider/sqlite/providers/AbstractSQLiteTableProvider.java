package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.customcontentprovider.sqlite.ISQLiteOperation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractSQLiteTableProvider<P extends AbstractSQLiteContentProvider> implements ISQLiteOperation {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(AbstractSQLiteTableProvider.class);

    @NonNull
    private final String tableName;

    @NonNull
    private final Class<P> contentProviderClass;

    public AbstractSQLiteTableProvider(@NonNull String tableName, @NonNull Class<P> contentProviderClass) {

        if (tableName.isEmpty())
            throw new IllegalArgumentException("tableName is empty");

        this.tableName = tableName;
        this.contentProviderClass = contentProviderClass;
    }

    @NonNull
    public final String getTableName() {
        return tableName;
    }

    @NonNull
    public final Uri getBaseUri(@NonNull Context context) {
        Uri baseUri = ProviderUtils.getContentProviderTableUri(context, context.getPackageName(), contentProviderClass, getTableName());
        if (baseUri == null) {
            throw new RuntimeException("can't make base URI");
        }
        return baseUri;
    }

    @NonNull
    public static <P extends AbstractSQLiteContentProvider> Uri getBaseUri(@NonNull Context context, @NonNull Class<P> contentProviderClass, @NonNull String tableName) {
        Uri baseUri = ProviderUtils.getContentProviderTableUri(context, context.getPackageName(), contentProviderClass, tableName);
        if (baseUri == null) {
            throw new RuntimeException("can't make base URI");
        }
        return baseUri;
    }

    /**
     * @return column name - type
     */
    @Nullable
    public abstract Map<String, Class<?>> getColumnsMap();

    public final void onCreate(SQLiteDatabase db) {
        execCreate(db);
    }

    private void execCreate(SQLiteDatabase db) {

        if (db == null) {
            throw new NullPointerException("db is null");
        }

        if (!db.isOpen()) {
            throw new IllegalStateException("db is not opened");
        }

        StringBuilder createScript = new StringBuilder();
        createScript.append("create table if not exists ");
        createScript.append(getTableName());
        createScript.append(" (");
        createScript.append(BaseColumns._ID);
        createScript.append(" integer primary key on conflict replace");

        Map<String, Class<?>> columnsMap = getColumnsMap();
        columnsMap = columnsMap != null? new LinkedHashMap<>(columnsMap) : new LinkedHashMap<String, Class<?>>();

            for (Map.Entry<String, Class<?>> column : columnsMap.entrySet()) {
                String columnName = column.getKey();
                Class<?> columnType = column.getValue();

                if (TextUtils.isEmpty(columnName)) {
                    throw new RuntimeException("one of specified column names is null or empty");
                }

                if (columnType == null) {
                    throw new RuntimeException("one of specified column types is null");
                }

                columnName = columnName.toLowerCase(Locale.getDefault());

                String columnTypeSql;

                if (columnType.isAssignableFrom(String.class)) {
                    columnTypeSql = "text";
                } else if (columnType.isAssignableFrom(Integer.class)) {
                    columnTypeSql = "integer";
                } else if (columnType.isAssignableFrom(Long.class)) {
                    columnTypeSql = "long";
                } else if (columnType.isAssignableFrom(Float.class)) {
                    columnTypeSql = "float";
                } else if (columnType.isAssignableFrom(Double.class)) {
                    columnTypeSql = "double";
                } else if (columnType.isAssignableFrom(byte[].class)) {
                    columnTypeSql = "blob";
                } else {
                    throw new UnsupportedOperationException("incorrect column class: " + columnType);
                }

                createScript.append(", ");
                createScript.append(columnName);
                createScript.append(" ");
                createScript.append(columnTypeSql);
            }

        createScript.append(")");
        logger.i(createScript.toString());
        db.execSQL(createScript.toString());
    }

    public Cursor query(SQLiteDatabase db, String[] columns, String where, String[] whereArgs, String orderBy) {
        return db.query(tableName, columns, where, whereArgs, null, null, orderBy);
    }

    public long insert(SQLiteDatabase db, ContentValues values) {
        return db.insert(tableName, BaseColumns._ID, values);
    }

    public int delete(SQLiteDatabase db, String where, String[] whereArgs) {
        return db.delete(tableName, where, whereArgs);
    }

    public int update(SQLiteDatabase db, ContentValues values, String where, String[] whereArgs) {
        return db.update(tableName, values, where, whereArgs);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + tableName + ";");
        onCreate(db);
    }

    @Override
    public void onContentChanged(Context context, SQLiteOperation operation, Bundle extras) {

    }

    @Nullable
    public static AbstractSQLiteTableProvider findSQLiteTableProvider(Collection<AbstractSQLiteTableProvider> providers, String tableName) {
        if (providers != null) {
            for (AbstractSQLiteTableProvider provider : providers) {
                if (provider != null && provider.getTableName().equalsIgnoreCase(tableName)) {
                    return provider;
                }
            }
        }
        return null;
    }

    public enum Order {
        ASC, DESC;

        public String getName() {
            return toString();
        }
    }

}

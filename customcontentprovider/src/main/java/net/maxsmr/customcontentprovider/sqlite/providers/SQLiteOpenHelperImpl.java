package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.maxsmr.commonutils.data.FileHelper;

public final class SQLiteOpenHelperImpl extends SQLiteOpenHelper {

    /**
     * @param databasePath if null or empty - default location will be used, otherwise - path on sdcard (for e.g.
     *                     Android/data/com.example.database)
     */
    public static SQLiteOpenHelperImpl createFrom(@NonNull Context context, @NonNull String databaseName, @Nullable String databasePath, int databaseVersion,
                                                  @NonNull Set<AbstractSQLiteTableProvider> tables) {

        if (TextUtils.isEmpty(databaseName))
            throw new IllegalArgumentException("databaseName is not specified");

        if (tables.isEmpty())
            throw new RuntimeException("tables is empty");

        if (databaseVersion < 0)
            throw new RuntimeException("incorrect databaseVersion: " + databaseVersion);

        if (!databaseName.toLowerCase().endsWith(".db")) {
            databaseName += ".db";
        }

        String targetName;

        if (!TextUtils.isEmpty(databasePath)) {
            targetName = FileHelper.testPath(databasePath, databaseName).getAbsolutePath();
        } else {
            targetName = databaseName;
        }

        return new SQLiteOpenHelperImpl(context, targetName, databaseVersion, tables);
    }

    @Nullable
    private SQLiteDatabase sqLiteDatabase;

    private final Set<AbstractSQLiteTableProvider> tableProviders = new LinkedHashSet<>();

    private SQLiteOpenHelperImpl(Context context, String databaseName, int databaseVersion,
                                 Set<AbstractSQLiteTableProvider> tableProviders) {
        super(context, databaseName, null, databaseVersion);
        this.tableProviders.addAll(tableProviders);
    }


    @Nullable
    public SQLiteDatabase getSqLiteDatabase() {
        return sqLiteDatabase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransactionNonExclusive();
        try {
            for (final AbstractSQLiteTableProvider table : tableProviders) {
                table.onCreate(db);
            }
            db.setTransactionSuccessful();
            dispatchDatabaseCreated(db);

        } finally {
            db.endTransaction();
            this.sqLiteDatabase = db;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransactionNonExclusive();
        try {
            for (final AbstractSQLiteTableProvider table : tableProviders) {
                table.onUpgrade(db, oldVersion, newVersion);
            }
            db.setTransactionSuccessful();
            dispatchDatabaseUpgraded(db);
        } finally {
            db.endTransaction();
            this.sqLiteDatabase = db;
        }
    }

    private final List<SQLiteCallbacks> callbacksList = new ArrayList<>();

    public void addSQLiteCallback(@NonNull SQLiteCallbacks callbacks) {
        if (!callbacksList.contains(callbacks)) {
            callbacksList.add(callbacks);
        }
    }

    public void removeSQLiteCallback(@NonNull SQLiteCallbacks callbacks) {
        if (callbacksList.contains(callbacks)) {
            callbacksList.remove(callbacks);
        }
    }

    private void dispatchDatabaseCreated(@NonNull SQLiteDatabase database) {
        for (SQLiteCallbacks callbacks : callbacksList) {
            callbacks.onDatabaseCreated(database);
        }
    }

    private void dispatchDatabaseUpgraded(@NonNull SQLiteDatabase database) {
        for (SQLiteCallbacks callbacks : callbacksList) {
            callbacks.onDatabaseUpgraded(database);
        }
    }

    public interface SQLiteCallbacks {
        void onDatabaseCreated(@NonNull SQLiteDatabase database);

        void onDatabaseUpgraded(@NonNull SQLiteDatabase database);
    }

}
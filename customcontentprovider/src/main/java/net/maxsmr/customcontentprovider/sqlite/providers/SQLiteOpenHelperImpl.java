package net.maxsmr.customcontentprovider.sqlite.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.Observable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class SQLiteOpenHelperImpl extends SQLiteOpenHelper {

    @NotNull
    private final Set<AbstractSQLiteTableProvider> tableProviders = new LinkedHashSet<>();

    @NotNull
    private final CallbacksObservable callbacksObservable = new CallbacksObservable();

    @Nullable
    private SQLiteDatabase sqLiteDatabase;

    private SQLiteOpenHelperImpl(Context context, String databaseName, int databaseVersion,
                                 Set<? extends AbstractSQLiteTableProvider> tableProviders) {
        super(context, databaseName, null, databaseVersion);
        this.tableProviders.addAll(tableProviders);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransactionNonExclusive();
        try {
            for (AbstractSQLiteTableProvider table : tableProviders) {
                if (CompareUtils.stringsEqual(db.getPath(), table.getTableName(), false)) {
                    table.onCreate(db);
                }
            }
            db.setTransactionSuccessful();
            callbacksObservable.dispatchDatabaseCreated(db);

        } finally {
            db.endTransaction();
            this.sqLiteDatabase = db;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransactionNonExclusive();
        try {
            for (AbstractSQLiteTableProvider table : tableProviders) {
                if (CompareUtils.stringsEqual(db.getPath(), table.getTableName(), false)) {
                    table.onUpgrade(db, oldVersion, newVersion);
                }
            }
            db.setTransactionSuccessful();
            callbacksObservable.dispatchDatabaseUpgraded(db);
        } finally {
            db.endTransaction();
            this.sqLiteDatabase = db;
        }
    }

    @NotNull
    public Set<AbstractSQLiteTableProvider> getTableProviders() {
        return new LinkedHashSet<>(tableProviders);
    }

    @Nullable
    public SQLiteDatabase getSQLiteDatabase() {
        return sqLiteDatabase;
    }

    public void addSQLiteCallback(@NotNull SQLiteCallbacks callbacks) {
        callbacksObservable.registerObserver(callbacks);
    }

    public void removeSQLiteCallback(@NotNull SQLiteCallbacks callbacks) {
        callbacksObservable.unregisterObserver(callbacks);
    }

    /**
     * @param databasePath if null or empty - default location will be used, otherwise - path on sdcard (for e.g.
     *                     Android/data/com.example.database)
     */
    @NotNull
    public static SQLiteOpenHelperImpl createFrom(
            @NotNull Context context,
            @NotNull String databaseName,
            @Nullable String databasePath,
            int databaseVersion,
            @NotNull Set<? extends AbstractSQLiteTableProvider> tableProviders) {

        if (TextUtils.isEmpty(databaseName))
            throw new IllegalArgumentException("databaseName is not specified");

        if (tableProviders.isEmpty())
            throw new RuntimeException("tableProviders is empty");

        if (databaseVersion < 0)
            throw new RuntimeException("incorrect databaseVersion: " + databaseVersion);

        if (!databaseName.toLowerCase(Locale.getDefault()).endsWith(".db")) {
            databaseName += ".db";
        }

        String targetName;

        if (!TextUtils.isEmpty(databasePath)) {
            targetName = FileHelper.checkPath(databasePath, databaseName).getAbsolutePath();
        } else {
            targetName = databaseName;
        }

        return new SQLiteOpenHelperImpl(context, targetName, databaseVersion, tableProviders);
    }

    public interface SQLiteCallbacks {

        void onDatabaseCreated(@NotNull SQLiteDatabase database);

        void onDatabaseUpgraded(@NotNull SQLiteDatabase database);
    }

    private static class CallbacksObservable extends Observable<SQLiteCallbacks> {

        private void dispatchDatabaseCreated(@NotNull SQLiteDatabase database) {
            synchronized (observers) {
                for (SQLiteCallbacks callbacks : observers) {
                    callbacks.onDatabaseCreated(database);
                }
            }
        }

        private void dispatchDatabaseUpgraded(@NotNull SQLiteDatabase database) {
            synchronized (observers) {
                for (SQLiteCallbacks callbacks : observers) {
                    callbacks.onDatabaseUpgraded(database);
                }
            }
        }
    }
}
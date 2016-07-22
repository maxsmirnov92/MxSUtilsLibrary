package net.maxsmr.customcontentprovider.sqlite;

import android.content.Context;
import android.os.Bundle;

public interface ISQLiteOperation {

    enum SQLiteOperation {

        INSERT(0), UPDATE(1), DELETE(2);

        private final int value;

        SQLiteOperation(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static SQLiteOperation fromNativeValue(int value) {

            for (SQLiteOperation op : SQLiteOperation.values()) {
                if (op.getValue() == value) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Incorrect native value " + value + " for enum type " + SQLiteOperation.class.getName());
        }

    }

    String KEY_LAST_ID = "ru.maxsmr.commoncontentprovider.sqlite.KEY_LAST_ID";

    String KEY_AFFECTED_ROWS = "ru.maxsmr.commoncontentprovider.sqlite.KEY_AFFECTED_ROWS";

    void onContentChanged(Context context, SQLiteOperation operation, Bundle extras);

}

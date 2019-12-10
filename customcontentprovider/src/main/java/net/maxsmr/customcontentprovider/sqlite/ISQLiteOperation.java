package net.maxsmr.customcontentprovider.sqlite;

import android.content.Context;
import android.os.Bundle;

public interface ISQLiteOperation {

    String KEY_LAST_ID = "ru.maxsmr.commoncontentprovider.sqlite.KEY_LAST_ID";

    String KEY_AFFECTED_ROWS = "ru.maxsmr.commoncontentprovider.sqlite.KEY_AFFECTED_ROWS";

    void onContentChanged(Context context, OperationType operation, Bundle extras);

    enum OperationType {

        INSERT(0), UPDATE(1), DELETE(2);

        private final int value;

        OperationType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static OperationType fromValue(int value) {

            for (OperationType op : OperationType.values()) {
                if (op.getValue() == value) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + OperationType.class.getName());
        }

    }

}

package net.maxsmr.networkutils;

import android.net.ConnectivityManager;

public enum NetworkType {

    NONE(-1),

    MOBILE(ConnectivityManager.TYPE_MOBILE),

    WIFI(ConnectivityManager.TYPE_WIFI);

    public final int value;

    NetworkType(int value) {
        this.value = value;
    }

    public static NetworkType fromValue(int value) throws IllegalArgumentException {

        for (NetworkType networkType : NetworkType.values()) {
            if (networkType.value == value) {
                return networkType;
            }
        }
        throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + NetworkType.class.getName());

    }

}

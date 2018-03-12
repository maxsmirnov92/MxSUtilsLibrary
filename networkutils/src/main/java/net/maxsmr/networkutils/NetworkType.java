package net.maxsmr.networkutils;

public enum NetworkType {

    NONE(-1),

    MOBILE(0),

    WIFI(1);

    private final int value;

    NetworkType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static NetworkType fromNativeValue(int value) throws IllegalArgumentException {

        for (NetworkType networkType : NetworkType.values()) {
            if (networkType.getValue() == value) {
                return networkType;
            }
        }
        throw new IllegalArgumentException("Incorrect native value " + value + " for enum type " + NetworkType.class.getName());

    }

}

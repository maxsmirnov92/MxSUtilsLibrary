package net.maxsmr.networkutils;

public enum NETWORK_TYPE {

    NONE(-1),

    MOBILE(0),

    WIFI(1);

    private final int value;

    NETWORK_TYPE(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static NETWORK_TYPE fromNativeValue(int value) throws IllegalArgumentException {

        for (NETWORK_TYPE networkType : NETWORK_TYPE.values()) {
            if (networkType.getValue() == value) {
                return networkType;
            }
        }
        throw new IllegalArgumentException("Incorrect native value " + value + " for enum type " + NETWORK_TYPE.class.getName());

    }

}

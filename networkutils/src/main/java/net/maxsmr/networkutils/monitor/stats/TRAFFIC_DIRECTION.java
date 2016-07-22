package net.maxsmr.networkutils.monitor.stats;


public enum TRAFFIC_DIRECTION {

    NONE(-1), RECEIVE(0), TRANSMIT(1), BOTH(2);

    private final int value;

    TRAFFIC_DIRECTION(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TRAFFIC_DIRECTION fromNativeValue(int value) throws IllegalArgumentException {

        switch (value) {
            case -1:
                return NONE;
            case 0:
                return RECEIVE;
            case 1:
                return TRANSMIT;
            case 2:
                return BOTH;
            default:
                throw new IllegalArgumentException("Incorrect native value for enum type " + TRAFFIC_DIRECTION.class.getName() + ": " + value);
        }
    }

}
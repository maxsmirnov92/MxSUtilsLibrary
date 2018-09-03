package net.maxsmr.networkutils.monitor.model;


public enum TrafficDirection {

    NONE(-1), RECEIVE(0), TRANSMIT(1), BOTH(2);

    private final int value;

    TrafficDirection(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TrafficDirection fromNativeValue(int value) throws IllegalArgumentException {

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
                throw new IllegalArgumentException("Incorrect native value for enum type " + TrafficDirection.class.getName() + ": " + value);
        }
    }

}
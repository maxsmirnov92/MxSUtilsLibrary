package net.maxsmr.commonutils.android.location.info;

public enum TrackingStatus {

    STOP_TRACKING(0),

    START_TRACKING(1),

    NEW_LOCATION(2);

    public final int value;

    TrackingStatus(int value) {
        this.value = value;
    }
}

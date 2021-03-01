package net.maxsmr.commonutils.gui;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

public abstract class OrientationIntervalListener extends OrientationEventListener {

    public static final int ROTATION_NOT_SPECIFIED = -1;

    public static final int NOTIFY_INTERVAL_NOT_SPECIFIED = -1;

    public static final int NOTIFY_DIFF_THRESHOLD_NOT_SPECIFIED = -1;

    private final long notifyInterval;

    private final int notifyDiffThreshold;

    private long lastNotifyTime = 0;

    private int lastRotation = ROTATION_NOT_SPECIFIED;

    private int lastCorrectedRotation = ROTATION_NOT_SPECIFIED;

    public OrientationIntervalListener(Context context) {
        this(context, SensorManager.SENSOR_DELAY_NORMAL, NOTIFY_INTERVAL_NOT_SPECIFIED, NOTIFY_DIFF_THRESHOLD_NOT_SPECIFIED);
    }

    public OrientationIntervalListener(Context context, long interval, int diffThreshold) {
        this(context, SensorManager.SENSOR_DELAY_NORMAL, interval, diffThreshold);
    }

    public OrientationIntervalListener(Context context, int rate, long interval, int diffThreshold) {
        super(context, rate);
        if (!(interval == NOTIFY_INTERVAL_NOT_SPECIFIED || interval > 0)) {
            throw new IllegalArgumentException("incorrect notify interval: " + interval);
        }
        if (!(diffThreshold == NOTIFY_DIFF_THRESHOLD_NOT_SPECIFIED || diffThreshold > 0)) {
            throw new IllegalArgumentException("incorrect notify diff threshold: " + interval);
        }
        notifyInterval = interval;
        notifyDiffThreshold = diffThreshold;
    }

    public long getLastNotifyTime() {
        return lastNotifyTime;
    }

    public int getLastRotation() {
        return lastRotation;
    }

    /**
     * user defined value
     */
    public int getLastCorrectedRotation() {
        return lastCorrectedRotation;
    }

    /**
     * @param lastCorrectedRotation user defined value
     */
    public void setLastCorrectedRotation(int lastCorrectedRotation) {
        if (lastCorrectedRotation == ROTATION_NOT_SPECIFIED || lastCorrectedRotation >= 0 && lastCorrectedRotation < 360) {
            this.lastCorrectedRotation = lastCorrectedRotation;
        }
    }

    @Override
    public void onOrientationChanged(int orientation) { // display orientation
        if (orientation >= 0 && orientation < 360) {
            if (orientation != lastRotation) {
                long currentTime = System.currentTimeMillis();
                int diff = 0;

                boolean intervalPassed = notifyInterval == NOTIFY_INTERVAL_NOT_SPECIFIED || lastNotifyTime == 0 || currentTime - lastNotifyTime >= notifyInterval;

                if (intervalPassed) {

                    if (lastRotation != ROTATION_NOT_SPECIFIED) {
                        if (orientation >= 0 && orientation < 90 && lastRotation >= 270 && lastRotation < 360) {
                            int currentRotationFixed = orientation + 360;
                            diff = Math.abs(currentRotationFixed - lastRotation);
                        } else if (orientation >= 270 && orientation < 360 && lastRotation >= 0 && lastRotation < 90) {
                            int lastRotationFixed = lastRotation + 360;
                            diff = Math.abs(orientation - lastRotationFixed);
                        } else {
                            diff = Math.abs(orientation - lastRotation);
                        }
                    }

                    if (lastRotation == ROTATION_NOT_SPECIFIED ||
                            notifyDiffThreshold == NOTIFY_DIFF_THRESHOLD_NOT_SPECIFIED || diff >= notifyDiffThreshold) {
                        doAction(orientation);
                        lastNotifyTime = currentTime;
                        lastRotation = orientation;
                    }
                }
            }
        }
    }

    protected abstract void doAction(int orientation);


}

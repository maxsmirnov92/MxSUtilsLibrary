package net.maxsmr.commonutils.hardware;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import net.maxsmr.commonutils.Observable;
import org.jetbrains.annotations.NotNull;

import kotlin.Pair;

public class SimpleGestureListener implements ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    public static final float MIN_SCALE_FACTOR = 1.0f;

    private final ScaleFactorChangeObservable listeners = new ScaleFactorChangeObservable();

    private final ScaleGestureDetector scaleGestureDetector;

    private double thresholdValue = MIN_SCALE_FACTOR;

    private double previousScaleFactor = MIN_SCALE_FACTOR, currentScaleFactor = previousScaleFactor;

    private boolean isScaling = false;

    public SimpleGestureListener(@NotNull Context ctx, float thresholdValue) {
        scaleGestureDetector = new ScaleGestureDetector(ctx, this);
        setThresholdValue(thresholdValue);
    }

    public void addScaleFactorChangeListener(@NotNull ScaleFactorChangeListener listener) {
        listeners.registerObserver(listener);
    }

    public void removeScaleFactorChangeListener(@NotNull ScaleFactorChangeListener listener) {
        listeners.unregisterObserver(listener);
    }

    public double getThresholdValue() {
        return thresholdValue;
    }

    /**
     * @param thresholdValue if {@link #MIN_SCALE_FACTOR} set, threshold will be ignored
     */
    public boolean setThresholdValue(float thresholdValue) {
        if (!isScaling) {
            if (thresholdValue < MIN_SCALE_FACTOR) {
                throw new IllegalArgumentException("incorrect threshold value: " + thresholdValue);
            }
            if (Double.compare(previousScaleFactor, thresholdValue) > 0 || Double.compare(currentScaleFactor, thresholdValue) > 0) {
                this.previousScaleFactor = this.currentScaleFactor = MIN_SCALE_FACTOR;
            }
            this.thresholdValue = thresholdValue;
            return true;
        }
        return false;
    }

    /**
     * @return {previous : current} pair
     */
    @NotNull
    public Pair<Double, Double> getScaleFactors() {
        return new Pair<>(previousScaleFactor, currentScaleFactor);
    }

    public boolean setTo(float scale) {
        if (!isScaling) {
            if (Double.compare(scale, MIN_SCALE_FACTOR) >= 0 && (Double.compare(thresholdValue, MIN_SCALE_FACTOR) == 0 || Double.compare(scale, thresholdValue) <= 0)) {
                previousScaleFactor = currentScaleFactor = scale;
            } else {
                throw new IllegalArgumentException("incorrect scale factor value: " + scale);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        double newScaleFactor;
        newScaleFactor = currentScaleFactor * detector.getScaleFactor();
        newScaleFactor = (newScaleFactor < MIN_SCALE_FACTOR ? MIN_SCALE_FACTOR : newScaleFactor); // prevent our view from becoming too small //
        newScaleFactor = ((float) ((int) (newScaleFactor * 100f))) / 100f; // Change precision to help with jitter when user just rests their fingers

        if (Double.compare(newScaleFactor, thresholdValue) <= 0) {
            previousScaleFactor = currentScaleFactor;
            currentScaleFactor = newScaleFactor;

            listeners.notifyScaleFactorChanged(previousScaleFactor, currentScaleFactor);
        }

        return isScaling = false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return isScaling = true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        isScaling = false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    public interface ScaleFactorChangeListener {
        void onScaleFactorChanged(double from, double to);
    }

    private static class ScaleFactorChangeObservable extends Observable<ScaleFactorChangeListener> {

        private void notifyScaleFactorChanged(double previousScaleFactor, double currentScaleFactor) {
            synchronized (observers) {
                for (ScaleFactorChangeListener l : observers) {
                    l.onScaleFactorChanged(previousScaleFactor, currentScaleFactor);
                }
            }
        }
    }
}
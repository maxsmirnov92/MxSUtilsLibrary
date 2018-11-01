package net.maxsmr.commonutils.android.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Observable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import net.maxsmr.commonutils.android.location.info.TrackingStatus;
import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LocationWatcher {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(LocationWatcher.class);

    private static LocationWatcher sInstance;

    public static void initInstance(Context ctx) {
        if (sInstance == null) {
            synchronized (LocationWatcher.class) {
                sInstance = new LocationWatcher(ctx);
            }
        }
    }

    public static LocationWatcher getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return sInstance;
    }

    public static final long DEFAULT_LOCATION_UPDATE_TIME = 5000;
    public static final float DEFAULT_LOCATION_UPDATE_DISTANCE = 0;

    private LocationWatcher(Context ctx) {
        mContext = ctx;
    }

    private final Context mContext;

    private final LocationObservable mLocationObservable = new LocationObservable();

    private final Set<String> mPreferredProviders = new LinkedHashSet<>();

    private DeviceLocationListener mLocationListener = null;

    private Location mLastLocation;

    private long mLocationUpdateTime = DEFAULT_LOCATION_UPDATE_TIME;

    private float mLocationUpdateDistance = DEFAULT_LOCATION_UPDATE_DISTANCE;

    public Observable<LocationTrackingListener> getLocationObservable() {
        return mLocationObservable;
    }

    public long getLocationUpdateTime() {
        return mLocationUpdateTime;
    }

    public float getLocationUpdateDistance() {
        return mLocationUpdateDistance;
    }

    @Nullable
    public Location getLastLocation() {
        return mLastLocation;
    }

    public void setPreferredProviders(@Nullable Collection<String> providers) {
        mPreferredProviders.clear();
        if (providers != null) {
            mPreferredProviders.addAll(providers);
        }
    }

    private boolean updateLocation(@NotNull Location location) {

        final boolean isSameLocationInfos = CompareUtils.objectsEqual(mLastLocation, location);
        logger.i("last location info: " + mLastLocation + ", new location info: " + location + ", is same: " + isSameLocationInfos);

        final float accuracy = mLastLocation == null ? 0 : mLastLocation.getAccuracy();
        final boolean isBetterAccuracy = location.getAccuracy() > accuracy;
        logger.i("last accuracy: " + accuracy + ", new is better: " + isBetterAccuracy);

        final long time = mLastLocation == null ? 0 : mLastLocation.getTime();
        final boolean isLaterTime = location.getTime() - time > mLocationUpdateTime;
        logger.i("last update time: " + time + ", new is later: " + isLaterTime);

        if (!isSameLocationInfos && (isBetterAccuracy || isLaterTime)) {

            mLastLocation = location;
            logger.i("last location info has been changed");

            mLocationObservable.dispatchLocationUpdated(mLastLocation);
            mLocationObservable.dispatchLocationTrackingStatusChanged(TrackingStatus.NEW_LOCATION);
            return true;
        }

        return false;
    }

    @SuppressWarnings("MissingPermission")
    private static boolean addLocationListener(Context ctx, boolean openGpsActivity, final long minTime,
                                               final float minDistance, Collection<String> preferredProviders, final LocationListener locationListener) {

        if (ctx == null)
            throw new NullPointerException("context is null");

        if (locationListener == null)
            throw new NullPointerException("locationListener is null");


        if (minTime < 0)
            throw new IllegalArgumentException("incorrect minTime: " + minTime);

        if (minDistance < 0)
            throw new IllegalArgumentException("incorrect minDistance: " + minDistance);

        final LocationManager locationManager = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        boolean result = false;

        if (preferredProviders != null) {
            Set<String> disabledProviders = new LinkedHashSet<>();
            result = true;
            for (String provider : preferredProviders) {
                if (!locationManager.isProviderEnabled(provider)) {
                    result = false;
                    disabledProviders.add(provider);
                }
            }
            if (!result) {
                logger.e("providers " + disabledProviders + " is not enabled");
                if (openGpsActivity)
                    H.startGpsSettingsActivity(ctx);
                return false;
            }
        }


        List<String> providers = locationManager.getProviders(true);
        if (providers != null) {
            for (final String provider : providers) {
                if (preferredProviders == null || preferredProviders.isEmpty() || preferredProviders.contains(provider)) {
                    if (LocationManager.GPS_PROVIDER.equals(provider)
                            || LocationManager.PASSIVE_PROVIDER.equals(provider)
                            || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                        locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener, ctx.getMainLooper());
                        result = true;
                    }
                }
            }
        }

        if (!result) {
            logger.e("no enabled providers");
            if (openGpsActivity)
                H.startGpsSettingsActivity(ctx);
        }

        return result;
    }

    @SuppressWarnings("MissingPermission")
    private static void removeLocationListener(Context ctx, final LocationListener locationListener) {

        if (locationListener == null)
            throw new NullPointerException("locationListener is null");

        ((LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE)).removeUpdates(locationListener);
    }

    private class DeviceLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            logger.d("onLocationChanged(), loc=" + loc);
            if (loc != null) {
                updateLocation(loc);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            logger.d("onProviderDisabled(), provider=" + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            logger.d("onProviderEnabled(), provider=" + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            logger.d("onStatusChanged(), provider=" + provider + ", status=" + status);

            switch (status) {
                case LocationProvider.AVAILABLE:
                    logger.i("provider " + provider + " is available");
                    break;

                case LocationProvider.OUT_OF_SERVICE:
                    logger.w("provider " + provider + " is out of service");
                    break;

                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    logger.w("provider " + provider + " is temporary unavailable");
                    break;
            }

        }
    }

    public boolean isTracking() {
        return mLocationListener != null;
    }

    public void stopTracking(boolean resetLastLoc) {
        logger.d("stopTracking(), resetLastLoc=" + resetLastLoc);

        if (!isTracking()) {
            logger.d("not tracking");
            return;
        }

        removeLocationListener(mContext, mLocationListener);
        mLocationListener = null;

        if (resetLastLoc) {
            mLastLocation = null;
        }

        mLocationObservable.dispatchLocationTrackingStatusChanged(TrackingStatus.STOP_TRACKING);
        logger.d("tracking stopped");
    }

    public void startTracking(long minTime, float minDistance, boolean resetLastLoc, boolean openGpsActivity) {
        if (!isTracking()) {
            restartTracking(minTime, minDistance, resetLastLoc, openGpsActivity);
        }
    }

    /**
     * restarts tracking if it was already started
     */
    public void restartTracking(long minTime, float minDistance, boolean resetLastLoc, boolean openGpsActivity) {
        logger.d("restartTracking(), minTime=" + minTime + ", minDistance=" + minDistance + ", resetLastLoc=" + resetLastLoc + ", openGpsActivity=" + openGpsActivity);

        stopTracking(resetLastLoc);

        if (addLocationListener(mContext, openGpsActivity, minTime, minDistance, mPreferredProviders, mLocationListener = new DeviceLocationListener())) {
            mLocationUpdateTime = minTime;
            mLocationUpdateDistance = minDistance;
            mLocationObservable.dispatchLocationTrackingStatusChanged(TrackingStatus.START_TRACKING);
            logger.d("tracking started");
        }
    }

    public static boolean isProviderEnabled(@NotNull Context context, @Nullable String provider) {
        if (TextUtils.isEmpty(provider)) {
            return false;
        }
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new RuntimeException(LocationManager.class.getSimpleName() + " is null");
        }
        return locationManager.isProviderEnabled(provider);
    }

    /**
     * @return requested {@linkplain Location} from any provider (best if more than one)
     * */
    @Nullable
    @SuppressLint("MissingPermission")
    public static Location getLastActualLocation(@NotNull Context context) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            throw new RuntimeException(LocationManager.class.getSimpleName() + " is null");
        }

        List<String> providers = locationManager.getProviders(true);

        Location bestLocation = null;

        for (String provider : providers) {
            logger.i("Provider: " + provider);
            if (!locationManager.isProviderEnabled(provider)) {
                logger.e("Provider " + provider + " is not enabled");
                continue;
            }
            try {
                final Location location = locationManager.getLastKnownLocation(provider);
                if (location != null &&
                        (bestLocation == null || bestLocation.getAccuracy() < location.getAccuracy())) {
                    bestLocation = location;
                }
            } catch (RuntimeException e) {
                logger.e("A RuntimeException occurred: " + e.getMessage(), e);
            }
        }

        return bestLocation;
    }

    private static class LocationObservable extends Observable<LocationTrackingListener> {

        private void dispatchLocationTrackingStatusChanged(TrackingStatus status) {
            for (LocationTrackingListener l : mObservers) {
                l.onLocationTrackingStatusChanged(status);
            }
        }

        private void dispatchLocationUpdated(Location location) {
            for (LocationTrackingListener l : mObservers) {
                l.onLocationUpdated(location);
            }
        }
    }

    public static class H {

        public static final float
                GAIA_CIRC_X = 40075.017f,
                GAIA_CIRC_Y = 40007.860f;

        /**
         * Very poor math function for converting Earth's degrees to kilometers.
         */
        public static double angularDistanceToKilometers(double x1, double y1, double x2, double y2) {
            x1 = x1 / 360.0 * GAIA_CIRC_X;
            x2 = x2 / 360.0 * GAIA_CIRC_X;
            y1 = y1 / 360.0 * GAIA_CIRC_Y;
            y2 = y2 / 360.0 * GAIA_CIRC_Y;

            double dX = Math.pow(x1 - x2, 2);
            double dY = Math.pow(y1 - y2, 2);

            return Math.sqrt(dX + dY);
        }

        public static void startGpsSettingsActivity(final Context ctx) {
            new Handler(ctx.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Intent gpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    gpsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(gpsIntent);
                }
            });
        }
    }

}

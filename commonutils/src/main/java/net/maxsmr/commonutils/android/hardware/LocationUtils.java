package net.maxsmr.commonutils.android.hardware;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.text.TextUtils;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LocationUtils {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(LocationUtils.class);

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

        String bestProvider = null;
        Location bestLocation = null;

        for (String provider : providers) {
            if (!locationManager.isProviderEnabled(provider)) {
                logger.w("Provider " + provider + " is not enabled");
                continue;
            }
            try {
                final Location location = locationManager.getLastKnownLocation(provider);
                if (location != null &&
                        (bestLocation == null || bestLocation.getAccuracy() < location.getAccuracy())) {
                    bestLocation = location;
                    bestProvider = provider;
                }
            } catch (RuntimeException e) {
                logger.e("A RuntimeException occurred: " + e.getMessage(), e);
            }
        }
        logger.i("Best location is " + bestLocation + " for provider " + bestProvider);
        return bestLocation;
    }

    public static boolean checkGpsEnabled(@NotNull Activity activity, int requestCode) {
        if (!isProviderEnabled(activity, LocationManager.GPS_PROVIDER)) {
            final Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (requestCode > 0) {
                activity.startActivityForResult(intent, requestCode);
            } else {
                activity.startActivity(intent);
            }
            return false;
        }
        return true;
    }
}

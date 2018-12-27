package net.maxsmr.commonutils.android.location;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class LocationHelper {

    private static final String URI_GEO = "geo:%1f,%2f";

    private static final float
            GAIA_CIRC_X = 40075.017f,
            GAIA_CIRC_Y = 40007.860f;


    @NotNull
    public static Intent getLocationOnMapIntent(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse(String.format(Locale.getDefault(), URI_GEO, latitude, longitude));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        return mapIntent;
    }


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

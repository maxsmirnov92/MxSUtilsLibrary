package net.maxsmr.commonutils.android.location;

import android.location.Location;

import net.maxsmr.commonutils.android.location.info.TrackingStatus;
import net.maxsmr.commonutils.android.location.info.LocationInfo;

public interface LocationTrackingListener {

    void onLocationUpdated(Location loc, LocationInfo locInfo);

    void onLocationTrackingStatusChanged(TrackingStatus status);
}

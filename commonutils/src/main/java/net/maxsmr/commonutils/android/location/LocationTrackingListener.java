package net.maxsmr.commonutils.android.location;

import android.location.Location;

import net.maxsmr.commonutils.android.location.info.TrackingStatus;

public interface LocationTrackingListener {

    void onLocationUpdated(Location loc);

    void onLocationTrackingStatusChanged(TrackingStatus status);
}

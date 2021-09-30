package net.maxsmr.commonutils.location;

import android.location.Location;

import net.maxsmr.commonutils.location.info.TrackingStatus;

@Deprecated
public interface LocationTrackingListener {

    void onLocationUpdated(Location loc);

    void onLocationTrackingStatusChanged(TrackingStatus status);
}

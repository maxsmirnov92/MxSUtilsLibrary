package net.maxsmr.commonutils.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Utils for converting Parcelable objects to and from byte arrays
 */
public class ParcelableUtils {

    @NonNull
    public static byte[] marshall(Parcelable parcelable) {
        if (parcelable != null) {
            Parcel parcel = Parcel.obtain();
            parcelable.writeToParcel(parcel, 0);
            byte[] bytes = parcel.marshall();
            parcel.recycle();
            return bytes;
        }
        return new byte[0];
    }

    @Nullable
    public static <T> T unmarshall(byte[] bytes, Parcelable.Creator<T> creator) {
        T result = null;
        Parcel parcel = unmarshall(bytes);
        if (parcel != null) {
            result = creator.createFromParcel(parcel);
            parcel.recycle();
        }
        return result;
    }

    @Nullable
    private static Parcel unmarshall(byte[] bytes) {
        Parcel parcel = null;
        if (bytes != null && bytes.length > 0) {
            parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // This is extremely important!
        }
        return parcel;
    }

}

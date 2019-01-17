package net.maxsmr.commonutils.data.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils for converting Parcelable objects to and from byte arrays
 */
public final class ParcelableUtils {

    private ParcelableUtils() {
        throw new AssertionError("no instances");
    }

    @NotNull
    public static byte[] marshall(@Nullable Parcelable parcelable) {
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
    public static <T> T unmarshall(@Nullable byte[] bytes, @NotNull Parcelable.Creator<T> creator) {
        T result = null;
        Parcel parcel = unmarshall(bytes);
        if (parcel != null) {
            result = creator.createFromParcel(parcel);
            parcel.recycle();
        }
        return result;
    }

    @Nullable
    public static Parcel unmarshall(@Nullable byte[] bytes) {
        Parcel parcel = null;
        if (bytes != null && bytes.length > 0) {
            parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0); // This is extremely important!
        }
        return parcel;
    }

    @NotNull
    public static <P extends Parcelable> List<P> getParcelableList(@Nullable Bundle args, @Nullable String argName, @NotNull Class<P> parcelableClass) {
        List<P> result = new ArrayList<>();
        if (args != null) {
            Parcelable[] array = args.getParcelableArray(argName);
            if (array != null) {
                for (Parcelable p : array) {
                    if (p != null && parcelableClass.isAssignableFrom(p.getClass())) {
                        //noinspection unchecked
                        result.add((P) p);
                    }
                }
            }
        }
        return result;
    }

    @Nullable
    public static <P extends Parcelable> P[] toParcelableArray(@Nullable List<P> parcelableList, @NotNull Class<P> pClass) {
        P[] array = null;
        if (parcelableList != null) {
            //noinspection unchecked
            array = parcelableList.toArray((P[]) Array.newInstance(pClass, parcelableList.size()));
        }
        return array;
    }

}

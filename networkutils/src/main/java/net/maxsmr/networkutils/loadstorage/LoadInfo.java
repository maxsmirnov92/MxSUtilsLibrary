package net.maxsmr.networkutils.loadstorage;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.ChecksumHelper;
import net.maxsmr.commonutils.data.model.InstanceManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;

import java.io.Serializable;

public class LoadInfo<B extends LoadRunnableInfo.Body> implements Serializable {

    private static final long serialVersionUID = 3809033529460650154L;

    public final int id;

    /**
     * name for serialized file
     */
    @NonNull
    public final String name;

    @NonNull
    public final B body;

    public LoadInfo(int id, @NonNull String name, @NonNull B body) {
//        if (id < 0) {
//            throw new IllegalArgumentException("incorrect id: " + id);
//        }
        this.id = id;
        this.name = name;
        this.body = body;
    }

    public String getMD5Hash() {
        return ChecksumHelper.getMD5Hash(this.toByteArray());
    }

    @Nullable
    public byte[] toByteArray() {
        return InstanceManager.asByteArray(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoadInfo<?> loadInfo = (LoadInfo<?>) o;

        if (id != loadInfo.id) return false;
        if (!name.equals(loadInfo.name)) return false;
        return body.equals(loadInfo.body);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + body.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LoadInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", body=" + body +
                '}';
    }

    @Nullable
    public static <I extends LoadInfo> I fromByteArray(byte[] byteArray, Class<I> clazz) throws IllegalArgumentException {
        return InstanceManager.fromByteArray(clazz, byteArray);
    }
}

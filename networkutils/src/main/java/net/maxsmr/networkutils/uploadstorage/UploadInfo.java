package net.maxsmr.networkutils.uploadstorage;

import android.support.annotation.Nullable;

import java.io.File;
import java.io.Serializable;

import net.maxsmr.commonutils.data.ChecksumHelper;
import net.maxsmr.commonutils.data.model.InstanceManager;


public class UploadInfo implements Serializable {

    private static final long serialVersionUID = 3809033529460650154L;

    public final int id;

    @Nullable
    public final File uploadFile;

//    @Nullable
//    public final String uploadName;

    public String getMD5Hash() {
        return ChecksumHelper.getMD5Hash(this.toByteArray());
    }

    @Nullable
    public byte[] toByteArray() {
        return InstanceManager.asByteArray(this);
    }

    @Nullable
    public static <I extends UploadInfo> I fromByteArray(byte[] byteArray, Class<I> clazz) throws IllegalArgumentException {
        return InstanceManager.fromByteArray(clazz, byteArray);
    }

    public UploadInfo(int id, @Nullable File uploadFile/*, @Nullable String uploadName*/) {
        this.id = id;
        this.uploadFile = uploadFile;
//        this.uploadName = uploadName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UploadInfo that = (UploadInfo) o;

        if (id != that.id) return false;
        return !(uploadFile != null ? !uploadFile.equals(that.uploadFile) : that.uploadFile != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (uploadFile != null ? uploadFile.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UploadInfo{" +
                "id=" + id +
                ", uploadFile=" + uploadFile +
                '}';
    }
}

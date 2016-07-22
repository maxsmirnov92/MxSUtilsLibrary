package net.maxsmr.networkutils.loadutil.executors.base.upload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import net.maxsmr.networkutils.loadstorage.LoadInfo;


public class WrappedUploadRunnableInfo<LI extends LoadInfo> extends UploadRunnableInfo {

    @NonNull
    public final LI loadInfo;

    public WrappedUploadRunnableInfo(@NonNull LI loadInfo, URL url, @NonNull LoadSettings settings, @Nullable List<Field> headerFields, @Nullable List<Field> formFields, @Nullable FileFormField fileFormField, Integer... acceptableResponseCodes) throws MalformedURLException {
        super(loadInfo.id, url, settings, headerFields, formFields, fileFormField, acceptableResponseCodes);
        this.loadInfo = loadInfo;
    }

    @Override
    public String toString() {
        return "WrappedUploadRunnableInfo{" +
                "loadInfo=" + loadInfo + ", " + super.toString() + "}";
    }
}

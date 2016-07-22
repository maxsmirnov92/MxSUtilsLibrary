package net.maxsmr.networkutils.loadutil.executors.base.download;

import android.support.annotation.NonNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import net.maxsmr.networkutils.loadstorage.LoadInfo;

public class WrappedDownloadRunnableInfo<LI extends LoadInfo> extends DownloadRunnableInfo {

    @NonNull
    public final LI loadInfo;

    public WrappedDownloadRunnableInfo(@NonNull LI loadInfo, URL url, File destFile, boolean overwriteExistingFile, boolean deleteUnfinishedFile, @NonNull LoadSettings settings) throws MalformedURLException {
        super(loadInfo.id, url, destFile, overwriteExistingFile, deleteUnfinishedFile, settings);
        this.loadInfo = loadInfo;
    }

    @Override
    public String toString() {
        return "WrappedDownloadRunnableInfo{" +
                "loadInfo=" + loadInfo + ", " + super.toString() + "}";
    }
}

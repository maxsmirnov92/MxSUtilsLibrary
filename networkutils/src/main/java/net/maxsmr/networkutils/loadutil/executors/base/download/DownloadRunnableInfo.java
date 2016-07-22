package net.maxsmr.networkutils.loadutil.executors.base.download;

import android.support.annotation.NonNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import net.maxsmr.networkutils.loadutil.executors.base.LoadRunnableInfo;


public class DownloadRunnableInfo extends LoadRunnableInfo {

    private static final long serialVersionUID = 2752232594822816967L;

    public final File destFile;
    public final boolean overwriteExistingFile;
    public final boolean deleteUnfinishedFile;

    public DownloadRunnableInfo(int id, URL url, File destFile, boolean overwriteExistingFile, boolean deleteUnfinishedFile, @NonNull LoadSettings settings) throws MalformedURLException {
        super(id, DownloadRunnableInfo.class.getSimpleName() + "_" + id, url, settings);
        this.destFile = destFile;
        this.overwriteExistingFile = overwriteExistingFile;
        this.deleteUnfinishedFile = deleteUnfinishedFile;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (deleteUnfinishedFile ? 1231 : 1237);
        result = prime * result + ((destFile == null) ? 0 : destFile.hashCode());
        result = prime * result + (overwriteExistingFile ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DownloadRunnableInfo other = (DownloadRunnableInfo) obj;
        if (deleteUnfinishedFile != other.deleteUnfinishedFile)
            return false;
        if (overwriteExistingFile != other.overwriteExistingFile)
            return false;
        if (destFile == null) {
            if (other.destFile != null)
                return false;
        } else if (!destFile.equals(other.destFile))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "DownloadRunnableInfo{" +
                "destFile=" + destFile +
                ", overwriteExistingFile=" + overwriteExistingFile +
                ", deleteUnfinishedFile=" + deleteUnfinishedFile +
                ", " + super.toString() +
                '}';
    }
}

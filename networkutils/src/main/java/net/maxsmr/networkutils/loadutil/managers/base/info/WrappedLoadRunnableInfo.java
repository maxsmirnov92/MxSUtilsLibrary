package net.maxsmr.networkutils.loadutil.managers.base.info;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.networkutils.loadstorage.LoadInfo;

import java.net.URL;

public final class WrappedLoadRunnableInfo<B extends LoadRunnableInfo.Body, LI extends LoadInfo<B>> extends LoadRunnableInfo {

    private WrappedLoadRunnableInfo(Builder<LI> builder) {
        super(builder);
        this.loadInfo = builder.loadInfo;
    }

    @NonNull
    public LI loadInfo;

    @Override
    public String toString() {
        return "WrappedDownloadRunnableInfo{" +
                "loadInfo=" + loadInfo + ", " + super.toString() + "}";
    }

    public static class Builder<LI extends LoadInfo> extends LoadRunnableInfo.Builder<WrappedLoadRunnableInfo> {

        @NonNull
        private final LI loadInfo;

        public Builder(URL url, @NonNull LoadSettings settings, @NonNull LI loadInfo) {
            super(loadInfo.id, url, settings);
            this.loadInfo = loadInfo;
            this.body(loadInfo.body);
        }

        @Override
        public void body(@Nullable Body body) {
            if (!CompareUtils.objectsEqual(body, loadInfo.body)) {
                throw new IllegalArgumentException("body must be same as loadInfo.body");
            }
            super.body(body);
        }

        @NonNull
        @SuppressWarnings("unchecked")
        @Override
        public WrappedLoadRunnableInfo build() throws ClassCastException {
            return new WrappedLoadRunnableInfo<>(this);
        }
    }
}

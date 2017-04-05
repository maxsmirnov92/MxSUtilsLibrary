package net.maxsmr.commonutils.android.gui.progressable;

import android.support.annotation.Nullable;
import android.view.View;

public class ImageProgressable implements Progressable {

    @Nullable
    public final View view;

    @Nullable
    private final View bar;

    public ImageProgressable(@Nullable View view, @Nullable View bar) {
        this.view = view;
        this.bar = bar;
    }

    @Override
    public void onStart() {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
        if (bar != null) {
            bar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        if (bar != null) {
            bar.setVisibility(View.GONE);
        }
    }
}

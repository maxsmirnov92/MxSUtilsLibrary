package net.maxsmr.commonutils.android.gui.progressable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

public class ImageProgressable implements Progressable {

    @NonNull
    private final ImageView imageView;

    @Nullable
    private final View bar;

    public ImageProgressable(@NonNull ImageView imageView, @Nullable View bar) {
        this.imageView = imageView;
        this.bar = bar;
    }

    @Override
    public void onStart() {
        imageView.setVisibility(View.GONE);
        if (bar != null) {
            bar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        imageView.setVisibility(View.VISIBLE);
        if (bar != null) {
            bar.setVisibility(View.GONE);
        }
    }
}

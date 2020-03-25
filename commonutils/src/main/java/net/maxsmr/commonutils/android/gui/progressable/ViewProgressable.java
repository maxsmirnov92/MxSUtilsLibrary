package net.maxsmr.commonutils.android.gui.progressable;

import android.view.View;

import org.jetbrains.annotations.Nullable;

@Deprecated
public class ViewProgressable implements Progressable {

    @Nullable
    public final View view;

    @Nullable
    private final View bar;

    public ViewProgressable(@Nullable View view, @Nullable View bar) {
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

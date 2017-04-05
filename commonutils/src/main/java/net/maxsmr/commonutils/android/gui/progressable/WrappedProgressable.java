package net.maxsmr.commonutils.android.gui.progressable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class WrappedProgressable implements Progressable {

    private final Set<Progressable> progressables = new LinkedHashSet<>();

    public WrappedProgressable(Progressable... progressables) {
        if (progressables != null) {
            this.progressables.addAll(Arrays.asList(progressables));
        }
    }

    @Override
    public void onStart() {
        if (isAlive()) {
            for (Progressable p : progressables) {
                if (p != null) {
                    p.onStart();
                }
            }
        }
    }

    @Override
    public void onStop() {
        if (isAlive()) {
            for (Progressable p : progressables) {
                if (p != null) {
                    p.onStop();
                }
            }
        }
    }

    protected abstract boolean isAlive();
}

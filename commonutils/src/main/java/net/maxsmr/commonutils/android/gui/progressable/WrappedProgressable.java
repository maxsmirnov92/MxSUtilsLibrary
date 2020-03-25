package net.maxsmr.commonutils.android.gui.progressable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Deprecated
public abstract class WrappedProgressable implements Progressable {

    private final Set<Progressable> progressables = new LinkedHashSet<>();

    private boolean isStarted = false;

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
            isStarted = true;
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
            isStarted = false;
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    protected abstract boolean isAlive();
}

package net.maxsmr.commonutils.android.gui.progressable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WrappedProgressable implements Progressable {

    private final List<Progressable> progressables = new ArrayList<>();

    public WrappedProgressable(Progressable... progressables) {
        if (progressables != null) {
            this.progressables.addAll(Arrays.asList(progressables));
        }
    }

    @Override
    public void onStart() {
        for (Progressable p : progressables) {
            if (p != null) {
                p.onStart();
            }
        }
    }

    @Override
    public void onStop() {
        for (Progressable p : progressables) {
            if (p != null) {
                p.onStop();
            }
        }
    }
}

package net.maxsmr.commonutils.android.gui.progressable;

import androidx.annotation.MainThread;

@Deprecated
public interface Progressable {

    Progressable STUB = new Progressable() {
        public void onStart() {
        }

        public void onStop() {
        }
    };


    @MainThread
    void onStart();

    @MainThread
    void onStop();

}

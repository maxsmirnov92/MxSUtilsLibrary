package net.maxsmr.commonutils.android.gui.fragments.alert;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class BaseProgressDialogFragment<L extends AlertDialogFragment.EventListener> extends AlertDialogFragment<L> {

    protected abstract int getProgressBarId();

    protected abstract int getLoadingMessageViewId();

    @Nullable
    protected ProgressBar progressBar;

    protected TextView loadingMessageView;

    @Override
    protected void onDialogCreated(@NotNull AlertDialog dialog) {
        super.onDialogCreated(dialog);
        if (customView != null) {
            final int progressBarId = getProgressBarId();
            if (progressBarId != 0) {
                progressBar = customView.findViewById(progressBarId);
            }
            final int loadingMessageId = getLoadingMessageViewId();
            if (loadingMessageId != 0) {
                loadingMessageView = customView.findViewById(loadingMessageId);
            }
        }
        if (loadingMessageView != null) {
            final String message = args.getString(Args.ARG_MESSAGE);
            loadingMessageView.setText(message);
        }
    }
}

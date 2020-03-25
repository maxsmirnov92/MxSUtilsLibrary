package net.maxsmr.commonutils.android.gui.progressable;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

@Deprecated
public class DialogProgressable implements Progressable, DialogInterface.OnKeyListener, DialogInterface.OnDismissListener, DialogInterface.OnCancelListener {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(DialogProgressable.class);
    
    @NotNull
    private final Context context;

    private ProgressDialog progressDialog;

    //    private final String defaultTitle;
    private String title;

    //    private final String defaultMessage;
    private String message;


    private boolean indeterminate = true;
    private int max = 0;

    private boolean cancelable = false;

    private int theme;

    private OnBackPressedListener backPressedListener;
    private DialogInterface.OnCancelListener cancelListener;
    private DialogInterface.OnDismissListener dismissListener;


    public DialogProgressable(@NotNull Context context) {
        this(context, null, null, false);
    }

//    public DialogProgressable(@NotNull Context context, boolean indeterminate, int max, boolean cancelable) {
//        this(context, null, context.getString(R.string.loading), indeterminate, max, cancelable);
//    }

    public DialogProgressable(@NotNull Context context, @Nullable String title, @Nullable String message, boolean cancelable) {
        this(context, title, message, true, 0, cancelable, 0);
    }


    public DialogProgressable(@NotNull Context context, @Nullable String title, @Nullable String message, boolean cancelable, int theme) {
        this(context, title, message, true, 0, cancelable, theme);
    }

    public DialogProgressable(@NotNull Context context, @Nullable String title, @Nullable String message, boolean indeterminate, int max, boolean cancelable) {
        this(context, title, message, indeterminate, max, cancelable, 0);
    }

    public DialogProgressable(@NotNull Context context, @Nullable String title, @Nullable String message, boolean indeterminate, int max, boolean cancelable, int theme) {
        this.context = context;
        setTitle(title);
        setMessage(message);
        setIndeterminate(indeterminate);
        setMax(max);
        setCancelable(cancelable);
    }

    public boolean isStarted() {
        return progressDialog != null && progressDialog.isShowing();
    }

    @MainThread
    private void start() {
        if (!isStarted()) {
            progressDialog = new ProgressDialog(context, theme);
            if (!indeterminate) {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            } else {
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
            progressDialog.setIndeterminate(indeterminate);
            progressDialog.setMax(max);
            progressDialog.setCancelable(cancelable);
            progressDialog.setOnKeyListener(this);
            progressDialog.setOnCancelListener(this);
            progressDialog.setOnDismissListener(this);
            progressDialog.setTitle(title);
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    @MainThread
    private void stop() {
        dismiss();
    }

    private void restart() {
        stop();
        start();
    }

    public void notifyProgress(int progress) {
        if (isStarted()) {
            if (!progressDialog.isIndeterminate()) {
                if (progress >= 0 && progress <= progressDialog.getMax()) {
                    progressDialog.setProgress(progress);
                } else {
                    throw new IllegalArgumentException("incorrect progress value: " + progress + " (max: " + progressDialog.getMax());
                }
            }
        }
    }

    @Nullable
    public ProgressBar getProgressBar() {
        if (!isStarted()) {
            throw new IllegalStateException("dialog was not started");
        }

        Field f = null;
        try {
            f = progressDialog.getClass().getDeclaredField("mProgress");
        } catch (NoSuchFieldException e) {
            logger.e(e);
        }

        if (f != null) {
            f.setAccessible(true);
            try {
                return (ProgressBar) f.get(progressDialog);
            } catch (IllegalAccessException e) {
                logger.e(e);
            }
        }

        return null;
    }

    @Nullable
    public TextView getMessageView() {

        if (!isStarted()) {
            throw new IllegalStateException("dialog was not started");
        }

        Field f = null;
        try {
            f = progressDialog.getClass().getDeclaredField("mMessageView");
        } catch (NoSuchFieldException e) {
            logger.e(e);
        }

        if (f != null) {
            f.setAccessible(true);
            try {
                return (TextView) f.get(progressDialog);
            } catch (IllegalAccessException e) {
                logger.e(e);
            }
        }

        return null;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public int getMax() {
        return max;
    }

    public DialogProgressable setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        if (isStarted()) {
            progressDialog.setIndeterminate(indeterminate);
        }
        return this;
    }

    public DialogProgressable setMax(int max) {
        this.max = max < 0 ? 0 : max;
        if (isStarted()) {
            progressDialog.setMax(this.max);
        }
        return this;
    }

    public boolean isCancelable() {
        return cancelable;
    }

    public DialogProgressable setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
        if (isStarted()) {
            progressDialog.setCancelable(cancelable);
        }
        return this;
    }

    public DialogProgressable setTheme(int theme) {
        this.theme = theme;
        if (isStarted()) {
            restart();
        }
        return this;
    }

    public boolean cancel() {
        if (isStarted()) {
            if (cancelable) {
                progressDialog.cancel();
                return true;
            }
        }
        return false;
    }

    public String getTitle() {
        return title;
    }

    public DialogProgressable setTitle(String title) {
        this.title = title;
        if (isStarted()) {
            progressDialog.setTitle(title);
        }
        return this;
    }

    public String getMessage() {
        return message;
    }

    public DialogProgressable setMessage(String message) {
        this.message = message;
        if (isStarted()) {
            progressDialog.setMessage(message);
        }
        return this;
    }

    public DialogProgressable setOnBackPressedListener(OnBackPressedListener listener) {
        this.backPressedListener = listener;
        return this;
    }

    public DialogProgressable setOnCancelListener(DialogInterface.OnCancelListener listener) {
        this.cancelListener = listener;
        return this;
    }

    public DialogProgressable setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.dismissListener = listener;
        return this;
    }

    private void dismiss() {
        if (isStarted()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    @MainThread
    public void onStart() {
        start();
    }

    @Override
    @MainThread
    public void onStop() {
        stop();
    }

    public interface OnBackPressedListener {
        void onBackPressed(DialogInterface dialog);
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && dialog != null) {
            if (backPressedListener != null) {
                backPressedListener.onBackPressed(dialog);
            }
        }
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dismissListener != null) {
            dismissListener.onDismiss(dialog);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (cancelListener != null) {
            cancelListener.onCancel(dialog);
        }
    }
}

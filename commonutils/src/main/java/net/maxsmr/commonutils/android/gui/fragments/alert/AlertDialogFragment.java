package net.maxsmr.commonutils.android.gui.fragments.alert;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlertDialogFragment<L extends AlertDialogFragment.EventListener> extends AppCompatDialogFragment implements DialogInterface.OnClickListener {

    private static AlertDialogFragment newInstance(@Nullable Bundle args) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    protected L eventListener;

    protected Bundle args;

    @Nullable
    private AlertID alertId = null;

    @Nullable
    protected View customView;

    @Nullable
    public AlertID getAlertId() {
        return alertId;
    }

    public void setAlertId(@Nullable AlertID alertId) {
        this.alertId = alertId;
    }

    public void setEventListener(@Nullable L eventListener) {
        this.eventListener = eventListener;
    }

    @Nullable
    public View getCustomView() {
        return customView;
    }

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = getArguments();
        if (args == null) {
            throw new IllegalStateException("specify " + AlertDialogFragment.class.getSimpleName() + " arguments");
        }
    }

    @Override
    @CallSuper
    public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState) {
        final LayoutInflater inflater = super.onGetLayoutInflater(savedInstanceState);

        AlertDialog dialog = getDialog();

        if (dialog == null) {
            throw new IllegalStateException(AlertDialog.class.getSimpleName() + " was not created");
        }

        onDialogCreated(dialog);
        if (eventListener != null) {
            eventListener.onDialogCreated(this, dialog);
        }
        return inflater;
    }

    @Nullable
    @Override
    public AlertDialog getDialog() {
        return (AlertDialog) super.getDialog();
    }

    @CallSuper
    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return createBuilder(args).create();
    }

    @CallSuper
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (eventListener != null) {
            eventListener.onDialogButtonClick(AlertDialogFragment.this, which);
        }
    }

    /** here you can setup your views */
    protected void onDialogCreated(@NotNull AlertDialog dialog) {
        setCancelable(args.getBoolean(Args.ARG_CANCELABLE, true));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (eventListener != null) {
            eventListener.onDialogDismiss(this);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (eventListener != null) {
            eventListener.onDialogCancel(this);
        }
    }

    /** override if want to create own {@linkplain AlertDialog.Builder} */
    @NotNull
    protected AlertDialog.Builder createBuilder(@NotNull Bundle args) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(args.getString(Args.ARG_TITLE));

        if (args.containsKey(Args.ARG_ICON_RES_ID)) {
            builder.setIcon(args.getInt(Args.ARG_ICON_RES_ID));
        }

        final int customViewId = args.getInt(Args.ARG_CUSTOM_VIEW_RES_ID);

        if (customViewId != 0) {
            customView = LayoutInflater.from(getContext()).inflate(customViewId, null);
            builder.setView(customView);
        } else {
            builder.setMessage(args.getString(Args.ARG_MESSAGE));
        }

        if (args.containsKey(Args.ARG_BUTTON_POSITIVE)) {
            builder.setPositiveButton(args.getString(Args.ARG_BUTTON_POSITIVE), this);
        }
        if (args.containsKey(Args.ARG_BUTTON_NEUTRAL)) {
            builder.setNeutralButton(args.getString(Args.ARG_BUTTON_NEUTRAL), this);
        }
        if (args.containsKey(Args.ARG_BUTTON_NEGATIVE)) {
            builder.setNegativeButton(args.getString(Args.ARG_BUTTON_NEGATIVE), this);
        }
        builder.setOnKeyListener((dialog, keyCode, event) -> eventListener != null && eventListener.onDialogKey(AlertDialogFragment.this, keyCode, event));
        return builder;
    }

    public static abstract class Builder<F extends AlertDialogFragment> {

        protected String title;
        protected String message;
        @DrawableRes
        protected int iconResId;
        @LayoutRes
        protected int customViewResId;
        protected boolean cancelable = true;
        protected String buttonPositive, buttonNeutral, buttonNegative;

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            this.iconResId = iconResId;
            return this;
        }

        public Builder setCustomView(int customViewResId) {
            this.customViewResId = customViewResId;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setButtons(String positive, String neutral, String negative) {
            buttonNegative = negative;
            buttonNeutral = neutral;
            buttonPositive = positive;
            return this;
        }

        @NotNull
        protected Bundle createArgs() {
            Bundle args = new Bundle();
            if (title != null) {
                args.putString(AlertDialogFragment.Args.ARG_TITLE, title);
            }
            if (message != null) {
                args.putString(AlertDialogFragment.Args.ARG_MESSAGE, message);
            }
            if (iconResId != 0) {
                args.putInt(Args.ARG_ICON_RES_ID, iconResId);
            }
            if (customViewResId != 0) {
                args.putInt(Args.ARG_CUSTOM_VIEW_RES_ID, customViewResId);
            }
            if (buttonPositive != null) {
                args.putString(AlertDialogFragment.Args.ARG_BUTTON_POSITIVE, buttonPositive);
            }
            if (buttonNeutral != null) {
                args.putString(Args.ARG_BUTTON_NEUTRAL, buttonNeutral);
            }
            if (buttonNegative != null) {
                args.putString(Args.ARG_BUTTON_NEGATIVE, buttonNegative);
            }
            args.putBoolean(AlertDialogFragment.Args.ARG_CANCELABLE, cancelable);
            return args;
        }

        public abstract F build();
    }

    public interface EventListener {

        void onDialogCreated(@NotNull AlertDialogFragment fragment, @NotNull AlertDialog dialog);

        void onDialogButtonClick(@NotNull AlertDialogFragment fragment, int which);

        boolean onDialogKey(@NotNull AlertDialogFragment fragment, int keyCode, KeyEvent event);

        void onDialogCancel(@NotNull AlertDialogFragment fragment);

        void onDialogDismiss(@NotNull AlertDialogFragment fragment);
    }

    public interface AlertID {

        int getId();
    }

    protected interface Args {

        String ARG_TITLE = Args.class.getName() + "#ARG_TITLE";
        String ARG_MESSAGE = Args.class.getName() + "#ARG_MESSAGE";
        String ARG_ICON_RES_ID = Args.class.getName() + "#ARG_ICON_RES_ID";
        String ARG_CUSTOM_VIEW_RES_ID = Args.class.getName() + "#ARG_CUSTOM_VIEW_RES_ID";
        String ARG_CANCELABLE = Args.class.getName() + "#ARG_CANCELABLE";
        String ARG_BUTTON_POSITIVE = Args.class.getName() + "#ARG_BUTTON_OK";
        String ARG_BUTTON_NEGATIVE = Args.class.getName() + "#ARG_BUTTON_NEGATIVE";
        String ARG_BUTTON_NEUTRAL = Args.class.getName() + "#ARG_BUTTON_NEUTRAL";
    }

    public static class DefaultBuilder extends Builder<AlertDialogFragment> {

        @Override
        public AlertDialogFragment build() {
            return newInstance(createArgs());
        }
    }
}
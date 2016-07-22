package net.maxsmr.commonutils.android.gui;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.maxsmr.commonutils.data.StringUtils;

public final class GuiUtils {

    public GuiUtils() {
        throw new AssertionError("no instances.");
    }

    public static void setBackgroundOn(Drawable background, View view) {
        if (background != null && view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(background);
            } else {
                view.setBackgroundDrawable(background);
            }
        }
    }

    public static int getStatusBarHeight(@NonNull Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @NonNull
    public static Pair<Integer, Integer> getImageViewDrawableSize(@NonNull ImageView imageView) {
        Drawable d = imageView.getDrawable();
        return d != null ? new Pair<>(d.getIntrinsicWidth(), d.getIntrinsicHeight()) : new Pair<>(0, 0);
    }

    @NonNull
    public static Pair<Integer, Integer> getRescaledImageViewSize(@NonNull ImageView imageView) {
        int ih, iw, iH, iW;
        ih = imageView.getMeasuredHeight(); //height of imageView
        iw = imageView.getMeasuredWidth(); //width of imageView
        Pair<Integer, Integer> dSize = getImageViewDrawableSize(imageView);
        iH = dSize.second; //original height of underlying image
        iW = dSize.first; //original width of underlying image
        if (iH != 0 && iW != 0) {
            if (ih / iH <= iw / iW) iw = iW * ih / iH; //rescaled width of image within ImageView
            else ih = iH * iw / iW; //rescaled height of image within ImageView;
        }
        return new Pair<>(iw, ih);
    }

    public static void setTextAndVisibility(TextView textView, CharSequence text) {
        if (StringUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }

    public static void setStatusBarColor(@ColorInt int color, @NonNull Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(color);
        }
    }

    public static void setNavigationBarColor(@ColorInt int color, @NonNull Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setNavigationBarColor(color);
        }
    }

    public static void setProgressBarColor(@ColorInt int color, @Nullable ProgressBar progressBar) {
        if (progressBar != null) {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mode = PorterDuff.Mode.MULTIPLY;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (progressBar.isIndeterminate() && progressBar.getIndeterminateDrawable() != null) {
                    progressBar.getIndeterminateDrawable().setColorFilter(color, mode);
                }
                if (!progressBar.isIndeterminate() && progressBar.getProgressDrawable() != null) {
                    progressBar.getProgressDrawable().setColorFilter(color, mode);
                }
            } else {
                ColorStateList stateList = ColorStateList.valueOf(color);
                progressBar.setIndeterminateTintMode(mode);
                progressBar.setProgressTintList(stateList);
                progressBar.setSecondaryProgressTintList(stateList);
                progressBar.setIndeterminateTintList(stateList);
                // new ColorStateList(new int[][]{new int[]{android.R.attr.state_enabled}}, new int[]{color})
            }
        }
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            hideKeyboard(activity, activity.getCurrentFocus());
        }
    }

    public static boolean hideKeyboard(Context context, View hostView) {
        return hostView != null && hostView.getWindowToken() != null && ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(hostView.getWindowToken(), 0);
    }

    public static boolean showKeyboard(Context mContext, EditText etText) {
        if (etText != null && etText.getWindowToken() != null) {
            etText.requestFocus();
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            return imm.showSoftInput(etText, 1);
        } else {
            return true;
        }
    }

    public static void toggleKeyboard(Context context) {
        ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(2, 0);
    }


    /**
     * Добавление слушателя {@link OnSoftInputStateListener} на состояние клавиатуры<br/>
     *
     * @param screenRootView           корневой {@link View} в разметке экрана
     * @param onSoftInputStateListener callback {@link OnSoftInputStateListener}
     */
    public static void addSoftInputStateListener(final View screenRootView, final OnSoftInputStateListener onSoftInputStateListener) {
        screenRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private static final int HEIGHT_ROOT_THRESHOLD = 100;

            @Override
            public void onGlobalLayout() {
                int heightDiff = screenRootView.getRootView().getHeight() - screenRootView.getHeight();
                if (heightDiff > HEIGHT_ROOT_THRESHOLD) {
                    if (onSoftInputStateListener != null) {
                        onSoftInputStateListener.onOpened();
                    }
                } else {
                    if (onSoftInputStateListener != null) {
                        onSoftInputStateListener.onClosed();
                    }
                }
            }
        });
    }

    public interface OnSoftInputStateListener {
        void onOpened();

        void onClosed();
    }

    /**
     * @return true if focused, false otherwise
     */
    public static boolean requestFocus(@Nullable View view, @Nullable Activity act) {
        if (view != null) {
            if (act != null) {
                if (view.isFocusable()) {
                    if (view.requestFocus()) {
                        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return true if focus cleared, false otherwise
     */
    public static boolean clearFocus(@Nullable View view, @Nullable Activity act) {
        if (view != null) {
            if (act != null) {
                view.clearFocus();
                act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if focus cleared, false otherwise
     */
    public static boolean clearFocus(@Nullable Activity act) {
        return clearFocus(act != null ? act.getCurrentFocus() : null, act);
    }

    public static void setEditTextHintByError(@NonNull TextInputLayout on, @Nullable String hint) {
        EditText et = on.getEditText();
        if (et != null) {
            et.setHint(TextUtils.isEmpty(on.getError()) ? null : hint);
        }
    }

    public static void setError(@Nullable String errorMsg, @NonNull TextInputLayout on, @Nullable Activity act) {
        on.setErrorEnabled(true);
        on.setError(errorMsg);
//        editText.getBackground().setColorFilter(act.getResources().getColor(R.color.textColorSecondary), PorterDuff.Mode.SRC_ATOP);
        on.refreshDrawableState();
        requestFocus(on.getEditText(), act);
    }

    public static void setErrorTextColor(TextInputLayout textInputLayout, int color) {
        try {
            Field fErrorView = TextInputLayout.class.getDeclaredField("mErrorView");
            fErrorView.setAccessible(true);
            TextView mErrorView = (TextView) fErrorView.get(textInputLayout);
            mErrorView.setTextColor(color);
            mErrorView.requestLayout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearError(boolean force, @NonNull TextInputLayout on) {
        if (force || !TextUtils.isEmpty(on.getError())) {
            on.setError(null);
            on.refreshDrawableState();
        }
    }

    public static void clearError(boolean force, @NonNull TextInputLayout on, EditText editText, @ColorInt int color) {
        if (force || !TextUtils.isEmpty(on.getError())) {
            on.setError(null);
            editText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            on.refreshDrawableState();
        }
    }

    public static class DisableErrorTextWatcher implements TextWatcher {

        private boolean isClearingEnabled = true;

        private final List<TextInputLayout> layouts = new ArrayList<>();

        /**
         * @param layouts TextInputLayouts to clear on editing
         */
        public DisableErrorTextWatcher(TextInputLayout... layouts) {
            if (layouts != null && layouts.length > 0) {
                this.layouts.addAll(new ArrayList<>(Arrays.asList(layouts)));
            }
        }

        public void setClearingEnabled(boolean enable) {
            isClearingEnabled = enable;
        }

        private static void clearLayout(TextInputLayout l) {
            if (l != null) {
                clearError(false, l);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            for (TextInputLayout l : layouts) {
                if (isClearingEnabled) {
                    clearLayout(l);
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    public static class EditTextKeyLimiter implements View.OnKeyListener {

        @NonNull
        final EditText et;

        final int linesLimit;

        public EditTextKeyLimiter(@NonNull EditText et, int linesLimit) {

            if (linesLimit <= 0) {
                throw new IllegalArgumentException("incorrect linesLimit: " + linesLimit);
            }

            this.et = et;
            this.linesLimit = linesLimit;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if (et == v) {

                // if enter is pressed start calculating
                if (keyCode == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_UP) {
                    String text = et.getText().toString().trim();

                    // find how many rows it cointains
                    int editTextRowCount = text.split("\\n").length;

                    // user has input more than limited - lets do something
                    // about that
                    if (editTextRowCount >= linesLimit) {

                        // find the last break
                        int lastBreakIndex = text.lastIndexOf("\n");

                        // compose new text
                        String newText = text.substring(0, lastBreakIndex);

                        // add new text - delete old one and append new one
                        // (append because I want the cursor to be at the end)
                        et.setText("");
                        et.append(newText);
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public static AlertDialog showAlertDialog(Context ctx, String title, String message, boolean cancelable, DialogInterface.OnClickListener posClickListener, DialogInterface.OnClickListener negClickListener, DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        return builder.setTitle(title).setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, posClickListener).setNegativeButton(android.R.string.no, negClickListener)
                .setOnCancelListener(cancelListener).setCancelable(cancelable).show();
    }

    public static AlertDialog showCustomAlertDialog(Context ctx, String title, String message, View content, boolean cancelable, DialogInterface.OnClickListener posClickListener, DialogInterface.OnClickListener negClickListener, DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        return builder.setTitle(title).setMessage(message)
                .setView(content).setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, posClickListener).setNegativeButton(android.R.string.no, negClickListener)
                .setOnCancelListener(cancelListener).setCancelable(cancelable).show();
    }

    @NonNull
    public static Point getCorrectedSurfaceViewSizeByPreviewSize(@NonNull Point viewSize, @NonNull Point previewSize) {

        if (viewSize.x <= 0 || viewSize.y <= 0) {
            throw new IllegalArgumentException("incorrect view size: " + viewSize.x + "x" + viewSize.y);
        }

        if (previewSize.x <= 0 || previewSize.y <= 0) {
            throw new IllegalArgumentException("incorrect preview size: " + previewSize.x + "x" + previewSize.y);
        }

        float ratio = (float) previewSize.x / (float) previewSize.y;

        float camHeight = (int) (viewSize.x * ratio);
        float newCamWidth;
        float newCamHeight;

        if (camHeight < viewSize.y) {
            float newHeightRatio = (float) viewSize.y / (float) previewSize.y;
            newCamWidth = viewSize.x * newHeightRatio;
            newCamHeight = (newHeightRatio * camHeight);
        } else {
            newCamWidth = viewSize.x;
            newCamHeight = camHeight;
        }

        return new Point((int) newCamWidth, (int) newCamHeight);
    }

    @NonNull
    public static Point getCorrectedSurfaceViewSizeByDisplaySize(@NonNull Context context, float previewProportion) {

        if (previewProportion <= 0) {
            throw new IllegalArgumentException("incorrect previewProportion: " + previewProportion);
        }

        Display display = ((WindowManager) (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return getCorrectedSurfaceViewSizeByPreviewProportion(previewProportion, metrics.heightPixels, FitSize.FIT_HEIGHT);
    }

    @NonNull
    public static Point getCorrectedSurfaceViewSizeByPreviewSize(@NonNull Context context, @NonNull Point previewSize, @NonNull Point viewSize) {

        if (previewSize.x <= 0 || previewSize.y <= 0) {
            throw new IllegalArgumentException("incorrect preview size: " + previewSize.x + "x" + previewSize.y);
        }

        float previewProportion;
        final GuiUtils.FitSize fitSize = GuiUtils.FitSize.FIT_HEIGHT;
        final int limitSize = viewSize.y;

        switch (context.getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                previewProportion = (float) previewSize.y / (float) previewSize.x;
                break;

            case Configuration.ORIENTATION_LANDSCAPE:
                previewProportion = (float) previewSize.x / (float) previewSize.y;
                break;

            default:
                throw new UnsupportedOperationException("unknown orientation: " + context.getResources().getConfiguration().orientation);
        }

        return getCorrectedSurfaceViewSizeByPreviewProportion(previewProportion, limitSize, fitSize);
    }

    @NonNull
    public static Point getCorrectedSurfaceViewSizeByPreviewProportion(float previewProportion, int limitSize, @NonNull FitSize fitSize) {

        if (limitSize <= 0) {
            throw new IllegalArgumentException("incorrect limit size: " + limitSize);
        }

        if (previewProportion == 0) {
            throw new IllegalArgumentException("incorrect preview proportion: " + previewProportion);
        }

        int newWidth;
        int newHeight;

        switch (fitSize) {
            case FIT_WIDTH:
                newWidth = limitSize;
                newHeight = Math.round((float) newWidth / previewProportion);
                break;

            case FIT_HEIGHT:
                newHeight = limitSize;
                newWidth = Math.round((float) newHeight * previewProportion);
                break;

            default:
                newWidth = 0;
                newHeight = 0;
                break;
        }

        return new Point(newWidth, newHeight);
    }

    public enum FitSize {
        FIT_WIDTH, FIT_HEIGHT;
    }


    public static void setViewSize(@NonNull View view, @NonNull Point size) {
        if (size.x < -1 || size.y < -1) {
            throw new IllegalArgumentException("incorrect view size: " + size.x + "x" + size.y);
        }
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = size.x;
        layoutParams.height = size.y;
        view.setLayoutParams(layoutParams);
    }

    @SuppressWarnings("PrimitiveArrayArgumentToVariableArgMethod")
    @Nullable
    public static Drawable getDrawableForState(@NonNull StateListDrawable stateListDrawable, int... state) {
        try {
            Method getStateDrawableIndex = StateListDrawable.class.getMethod("getStateDrawableIndex", int[].class);
            Method getStateDrawable = StateListDrawable.class.getMethod("getStateDrawable", int.class);
            getStateDrawableIndex.setAccessible(true);
            getStateDrawable.setAccessible(true);
            int index = (int) getStateDrawableIndex.invoke(stateListDrawable, state);
            return (Drawable) getStateDrawable.invoke(stateListDrawable, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void collapseToolbar(CoordinatorLayout rootLayout, View coordinatorChild, AppBarLayout appbarLayout) {
        boolean found = false;
        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            if (rootLayout.getChildAt(i) == coordinatorChild) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("view "+ coordinatorChild + " is not a child of " + rootLayout);
        }
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) coordinatorChild.getLayoutParams();
        AppBarLayout.ScrollingViewBehavior behavior = (AppBarLayout.ScrollingViewBehavior) params.getBehavior();
        if (behavior != null) {
            behavior.onNestedFling(rootLayout, appbarLayout, null, 0, 10000, true);
        }
    }

    public static class ProtectRangeInputFilter implements InputFilter {

        final int startIndex;
        final int endIndex;

        public ProtectRangeInputFilter(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (dstart >= startIndex && dstart <= endIndex)
                return source.length() == 0 ? dest.subSequence(dstart, dend) : "";

            return source;
        }
    }

}

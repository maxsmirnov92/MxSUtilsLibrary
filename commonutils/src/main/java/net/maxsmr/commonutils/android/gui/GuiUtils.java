package net.maxsmr.commonutils.android.gui;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.maxsmr.commonutils.data.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.maxsmr.commonutils.android.gui.OrientationIntervalListener.ROTATION_NOT_SPECIFIED;

public final class GuiUtils {

    public GuiUtils() {
        throw new AssertionError("no instances.");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <V extends View> V findViewById(@Nullable View view, @IdRes int id) throws ClassCastException {
        return view != null ? (V) view.findViewById(id) : null;
    }

    @SuppressWarnings("deprecation")
    public static void setBackground(Drawable background, View view) {
        if (background != null && view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(background);
            } else {
                view.setBackgroundDrawable(background);
            }
        }
    }

    public int getDimensionFromAttr(@NonNull Context context, int attr) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{attr});
        try {
            return (int) styledAttributes.getDimension(0, 0);
        } finally {
            styledAttributes.recycle();
        }
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

    @SuppressWarnings("unchecked")
    public static void setBottomSheetHideable(@NonNull BottomSheetDialog dialog, boolean toggle) {
        Class<BottomSheetDialog> clazz = (Class<BottomSheetDialog>) dialog.getClass();
        Field behaviourField = null;
        try {
            behaviourField = clazz.getDeclaredField("mBehavior");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (behaviourField != null) {
            behaviourField.setAccessible(true);
            BottomSheetBehavior<FrameLayout> behavior = null;
            try {
                behavior = (BottomSheetBehavior<FrameLayout>) behaviourField.get(dialog);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (behavior != null) {
                behavior.setHideable(toggle);
            }
        }
    }

    public static boolean isKeyboardShowed(View view) {
        if (view == null) {
            return false;
        }
        try {
            InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            return inputManager.isActive(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    public static void setHomeButtonEnabled(@NonNull AppCompatActivity activity, boolean toggle) {
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayShowHomeEnabled(toggle);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(toggle);
        }
    }

    public static void setFullScreen(Activity activity, boolean toggle) {
        if (toggle) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        activity.getWindow().getDecorView().requestLayout();
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
//                        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
//                act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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

    public static int getViewInset(View view) {

        if (view == null) {
            return 0;
        }

        int statusBarHeight = getStatusBarHeight(view.getContext());

        if (statusBarHeight < 0) {
            return 0;
        }

        DisplayMetrics dm = view.getContext().getResources().getDisplayMetrics();

        if (Build.VERSION.SDK_INT < 21 || view.getHeight() == dm.heightPixels || view.getHeight() == dm.heightPixels - statusBarHeight) {
            return 0;
        }
        try {
            Field mAttachInfoField = View.class.getDeclaredField("mAttachInfo");
            mAttachInfoField.setAccessible(true);
            Object mAttachInfo = mAttachInfoField.get(view);
            if (mAttachInfo != null) {
                Field mStableInsetsField = mAttachInfo.getClass().getDeclaredField("mStableInsets");
                mStableInsetsField.setAccessible(true);
                Rect insets = (Rect) mStableInsetsField.get(mAttachInfo);
                return insets.bottom;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return 0;
    }

    public static int getStatusBarHeight(@NonNull Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static int getKeyboardHeight(@NonNull View rootView, @NonNull View targetView) {
        Rect rect = new Rect();
        targetView.getWindowVisibleDisplayFrame(rect);
        int usableViewHeight = rootView.getHeight() - (rect.top != 0 ? getStatusBarHeight(rootView.getContext()) : 0) - getViewInset(rootView);
        return usableViewHeight - (rect.bottom - rect.top);

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
    public static Point getFixedViewSizeByDisplay(@NonNull Context context, @NonNull Point targetSize) {
        return getFixedViewSizeByDisplay(context, (float) targetSize.x / targetSize.y);
    }

    @NonNull
    public static Point getFixedViewSizeByDisplay(@NonNull Context context, float targetScale) {
        Display display = ((WindowManager) (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        Point screenSize = new Point(metrics.widthPixels, metrics.heightPixels);
        return getFixedViewSize(targetScale, screenSize);
    }

    @NonNull
    public static Point getFixedViewSize(@NonNull Point targetSize, @NonNull View view) {
        return getFixedViewSize(targetSize, new Point(view.getMeasuredWidth(), view.getMeasuredHeight()));
    }

    @NonNull
    public static Point getFixedViewSize(@NonNull Point targetSize, @Nullable Point measuredViewSize) {
        return getFixedViewSize(targetSize, measuredViewSize, null);
    }

    @NonNull
    public static Point getFixedViewSize(@NonNull Point targetSize, @Nullable Point measuredViewSize, @Nullable Point maxViewSize) {
        return getFixedViewSize((float) targetSize.x / targetSize.y, measuredViewSize, maxViewSize);
    }

    @NonNull
    public static Point getFixedViewSize(float targetScale, @Nullable Point measuredViewSize) {
        return getFixedViewSize(targetScale, measuredViewSize, null);
    }

    @NonNull
    public static Point getFixedViewSize(float targetScale, @Nullable Point measuredViewSize, @Nullable Point maxViewSize) {
        if (targetScale < 0) {
            throw new IllegalArgumentException("targetScale < 0");
        }

        Point newViewSize = new Point();

        if (targetScale == 0) {
            return newViewSize;
        }

        if (measuredViewSize == null) {
            return newViewSize;
        }
        if (measuredViewSize.x < 0 || measuredViewSize.y < 0) {
            throw new IllegalArgumentException("incorrect view size: " + measuredViewSize.x + "x" + measuredViewSize.y);
        }

        if (measuredViewSize.x == 0) {
            if (maxViewSize == null || maxViewSize.x <= 0) {
                return newViewSize;
            }
            measuredViewSize.x = maxViewSize.x;
        }

        if (measuredViewSize.y == 0) {
            if (maxViewSize == null || maxViewSize.y <= 0) {
                return newViewSize;
            }
            measuredViewSize.y = maxViewSize.y;
        }

        float viewScale = (float) measuredViewSize.x / measuredViewSize.y;

        if (viewScale <= targetScale) {
            newViewSize.x = measuredViewSize.x;
            newViewSize.y = Math.round((float) newViewSize.x / targetScale);
        } else {
            newViewSize.y = measuredViewSize.y;
            newViewSize.x = Math.round((float) newViewSize.y * targetScale);
        }

        return newViewSize;
    }

    @NonNull
    public static Point fixViewSize(Point targetSize, @Nullable View view) {
        return fixViewSize(targetSize, view, null);
    }

    @NonNull
    public static Point fixViewSize(float targetScale, @Nullable View view) {
        return fixViewSize(targetScale, view, null);
    }

    @NonNull
    public static Point fixViewSize(Point targetSize, @Nullable View view, @Nullable Point maxViewSize) {
        return fixViewSize(targetSize != null ? (float) targetSize.x / targetSize.y : 0, view, maxViewSize);
    }

    @NonNull
    public static Point fixViewSize(float targetScale, @Nullable View view, @Nullable Point maxViewSize) {
        Point fixedSize = new Point();
        if (view != null) {
            fixedSize = getFixedViewSize(targetScale, new Point(view.getMeasuredWidth(), view.getMeasuredHeight()), maxViewSize);
        } else {
            return fixedSize;
        }
        if (fixedSize.x > 0 && fixedSize.y > 0) {
            setViewSize(view, fixedSize);
        }
        return fixedSize;
    }

    @NonNull
    public static Point getAutoScaledSize(@NonNull View view, @Nullable Point maxViewSize, int fixedSize) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        width = width <= 0 ? (maxViewSize != null ? maxViewSize.x : 0) : width;
        height = height <= 0 ? (maxViewSize != null ? maxViewSize.y : 0) : height;
        return getAutoScaledSize(new Point(width, height), fixedSize);
    }

    @NonNull
    public static Point getAutoScaledSize(@NonNull Point size, int fixedSize) {
        return getAutoScaledSize((float) size.x / size.y, fixedSize);
    }

    @NonNull
    public static Point getAutoScaledSize(float scale, int fixedSize) {
        return getScaledSize(scale, fixedSize, scale > 1.0f);
    }

    @NonNull
    public static Point getScaledSize(@NonNull View view, @Nullable Point maxViewSize, int fixedSize, boolean isWidth) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        width = width <= 0 ? (maxViewSize != null ? maxViewSize.x : 0) : width;
        height = height <= 0 ? (maxViewSize != null ? maxViewSize.y : 0) : height;
        return getScaledSize(new Point(width, height), fixedSize, isWidth);
    }

    @NonNull
    public static Point getScaledSize(@NonNull Point size, int fixedSize, boolean isWidth) {
        if (size.x <= 0 || size.y <= 0) {
            throw new IllegalArgumentException("incorrect size: " + size.x + "x" + size.y);
        }
        return getScaledSize((float) size.x / size.y, fixedSize, isWidth);
    }

    @NonNull
    public static Point getScaledSize(float scale, int fixedSize, boolean isWidth) {
        if (scale <= 0) {
            throw new IllegalArgumentException("incorrect scale: " + scale);
        }
        if (fixedSize <= 0) {
            throw new IllegalArgumentException("incorrect fixedSize: " + scale);
        }
        Point result = new Point();

        if (isWidth) {
            result.x = fixedSize;
            result.y = (int) (fixedSize / scale);
        } else {
            result.x = (int) (fixedSize * scale);
            result.y = fixedSize;
        }
        return result;
    }

    @NonNull
    public static android.support.v4.util.Pair<Integer, Integer> calcAspectRatioFor(int width, int height) {
        double aspectRatio = (double) width / (double) height;
        int dividend = width > height ? width : height;
        int divider = width > height ? height : width;
        int scale = 2;
        while (scale <= 9) {
            double scaledDividend = (double) dividend / (double) scale;
            double scaledDivider = (double) divider / (double) scale;
            double diff1 = (int) scaledDividend - scaledDividend;
            double diff2 = (int) scaledDivider - scaledDivider;
            if (diff1 == 0 && diff2 == 0) {
                dividend = (int) scaledDividend;
                divider = (int) scaledDivider;
                scale = 2;
            } else {
                scale++;
            }
        }
        android.support.v4.util.Pair<Integer, Integer> result;
        if (width > height) {
            result = new android.support.v4.util.Pair<>(dividend, divider);
        } else {
            result = new android.support.v4.util.Pair<>(divider, dividend);
        }
        return result;
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

    public static void collapseToolbar(CoordinatorLayout rootLayout, View coordinatorChild, AppBarLayout appbarLayout) {
        boolean found = false;
        for (int i = 0; i < rootLayout.getChildCount(); i++) {
            if (rootLayout.getChildAt(i) == coordinatorChild) {
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("view " + coordinatorChild + " is not a child of " + rootLayout);
        }
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) coordinatorChild.getLayoutParams();
        AppBarLayout.ScrollingViewBehavior behavior = (AppBarLayout.ScrollingViewBehavior) params.getBehavior();
        if (behavior != null) {
            behavior.onNestedFling(rootLayout, appbarLayout, null, 0, 10000, true);
        }
    }

    public static int getCurrentDisplayOrientation(@NonNull Context context) {
        int degrees = 0;
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

    public static int getCorrectedDisplayRotation(int rotation) {
        rotation = rotation % 360;
        int result = ROTATION_NOT_SPECIFIED;
        if (rotation >= 315 && rotation < 360 || rotation >= 0 && rotation < 45) {
            result = 0;
        } else if (rotation >= 45 && rotation < 135) {
            result = 90;
        } else if (rotation >= 135 && rotation < 225) {
            result = 180;
        } else if (rotation >= 225 && rotation < 315) {
            result = 270;
        }
        return result;
    }

    public static boolean copyToClipboard(@NonNull Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
            return true;
        }
        return false;
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

}

package net.maxsmr.commonutils.android.gui;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.maxsmr.commonutils.android.gui.OrientationIntervalListener.ROTATION_NOT_SPECIFIED;
import static net.maxsmr.commonutils.data.SymbolConstKt.NEXT_LINE;

public final class GuiUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(GuiUtils.class);

    public GuiUtils() {
        throw new AssertionError("no instances.");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <V extends View> V findViewById(@Nullable View view, @IdRes int id) throws ClassCastException {
        return view != null ? (V) view.findViewById(id) : null;
    }

    @SuppressWarnings("deprecation")
    public static void setWindowBackground(Drawable background, View view) {
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(background);
            } else {
                view.setBackgroundDrawable(background);
            }
        }
    }

    @NotNull
    public static Pair<Integer, Integer> getImageViewDrawableSize(@NotNull ImageView imageView) {
        Drawable d = imageView.getDrawable();
        return d != null ? new Pair<>(d.getIntrinsicWidth(), d.getIntrinsicHeight()) : new Pair<>(0, 0);
    }

    @NotNull
    public static Pair<Integer, Integer> getRescaledImageViewSize(@NotNull ImageView imageView) {
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

    /**
     * Меняет цвет иконок статусбара на темный/светлый
     *
     * @param v      корневая {@link View} лейаута
     * @param toogle {@link Boolean#TRUE} тёмные icons, {@link Boolean#FALSE} - светлые
     */
    public static void toggleLightStatusBar(View v, boolean toogle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                //noinspection ConstantConditions
                int flags = v.getSystemUiVisibility();
                flags = toogle ? flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                v.setSystemUiVisibility(flags);
            } catch (Exception ignored) {
            }
        }
    }

    public static void setProgressBarColor(@ColorInt int color, @Nullable ProgressBar progressBar) {
        if (progressBar != null) {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
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

    /**
     * Задает локаль по умолчанию в рамках приложения.
     *
     * @param locale объект локали
     */
    public static void forceLocaleInApp(@NotNull Context context, Locale locale) {
        Locale.setDefault(locale);

        Resources resources;
        resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale);
            context.createConfigurationContext(configuration);
        } else {
            //noinspection deprecation // есть проверка
            configuration.locale = locale;
            //noinspection deprecation // есть проверка
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }

    @SuppressWarnings("unchecked")
    public static void setBottomSheetHideable(@NotNull BottomSheetDialog dialog, boolean toggle) {
        Class<BottomSheetDialog> clazz = (Class<BottomSheetDialog>) dialog.getClass();
        Field behaviourField = null;
        try {
            behaviourField = clazz.getDeclaredField("mBehavior");
        } catch (NoSuchFieldException e) {
            logger.e(e);
        }
        if (behaviourField != null) {
            behaviourField.setAccessible(true);
            BottomSheetBehavior<FrameLayout> behavior = null;
            try {
                behavior = (BottomSheetBehavior<FrameLayout>) behaviourField.get(dialog);
            } catch (IllegalAccessException e) {
                logger.e(e);
            }
            if (behavior != null) {
                behavior.setHideable(toggle);
            }
        }
    }

    public static boolean isKeyboardShown(Activity activity) {
        return isKeyboardShown(activity.getCurrentFocus());
    }

    public static boolean isKeyboardShown(View view) {
        if (view == null) {
            return false;
        }
        try {
            InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            return inputManager.isActive(view);
        } catch (Exception e) {
            logger.e(e);
        }
        return false;
    }

    public static void showKeyboard(Activity activity) {
        if (activity != null) {
            showKeyboard(activity.getCurrentFocus());
        }
    }

    public static boolean showKeyboard(View hostView) {
        if (hostView != null && hostView.getWindowToken() != null) {
            InputMethodManager imm = (InputMethodManager) hostView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            return imm.showSoftInput(hostView, InputMethodManager.SHOW_IMPLICIT);
        } else {
            return true;
        }
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null) {
            hideKeyboard(activity.getCurrentFocus());
        }
    }

    public static boolean hideKeyboard(View hostView) {
        return hostView != null && hostView.getWindowToken() != null
                && ((InputMethodManager) hostView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(hostView.getWindowToken(), 0);
    }

    public static void toggleKeyboard(Activity activity) {
        toggleKeyboard(activity.getCurrentFocus());
    }

    public static void toggleKeyboard(View hostView) {
        if (isKeyboardShown(hostView)) {
            hideKeyboard(hostView);
        } else {
            showKeyboard(hostView);
        }
    }

    public static void setHomeButtonEnabled(@NotNull AppCompatActivity activity, boolean toggle) {
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
    public static boolean requestFocus(@Nullable View view/*, @Nullable Activity act*/) {
        if (view != null) {
//            if (act != null) {
            if (view.isFocusable()) {
                if (view.requestFocus()) {
//                        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    return true;
                }
//                }
            }
        }
        return false;
    }

    /**
     * @return true if focus cleared, false otherwise
     */
    public static boolean clearFocus(@Nullable View view) {
        if (view != null) {
            view.clearFocus();
//                act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            return true;
        }
        return false;
    }

    /**
     * @return true if focus cleared, false otherwise
     */
    public static boolean clearFocus(@Nullable Activity act) {
        return clearFocus(act != null ? act.getCurrentFocus() : null);
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
            logger.e(e);

        }
        return 0;
    }

    public static int getStatusBarHeight(@NotNull Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public static int getKeyboardHeight(@NotNull View rootView, @NotNull View targetView) {
        Rect rect = new Rect();
        targetView.getWindowVisibleDisplayFrame(rect);
        int usableViewHeight = rootView.getHeight() - (rect.top != 0 ? getStatusBarHeight(rootView.getContext()) : 0) - getViewInset(rootView);
        return usableViewHeight - (rect.bottom - rect.top);

    }

    public static void setEditTextHintByError(@NotNull TextInputLayout on, @Nullable String hint) {
        EditText et = on.getEditText();
        if (et != null) {
            et.setHint(TextUtils.isEmpty(on.getError()) ? null : hint);
        }
    }

    public static void setError(@Nullable String errorMsg, @NotNull TextInputLayout on) {
        on.setErrorEnabled(true);
        on.setError(errorMsg);
//        editText.getBackground().setColorFilter(act.getResources().getColor(R.color.textColorSecondary), PorterDuff.Mode.SRC_ATOP);
        on.refreshDrawableState();
        requestFocus(on.getEditText());
    }

    @Deprecated
    public static void setErrorTextColor(TextInputLayout textInputLayout, int color) {
        try {
            Field fErrorView = TextInputLayout.class.getDeclaredField("mErrorView");
            fErrorView.setAccessible(true);
            TextView mErrorView = (TextView) fErrorView.get(textInputLayout);
            mErrorView.setTextColor(color);
            mErrorView.requestLayout();
        } catch (Exception e) {
            logger.e(e);
        }
    }

    public static void clearError(boolean force, @NotNull TextInputLayout on) {
        if (force || !TextUtils.isEmpty(on.getError())) {
            on.setError(null);
            on.refreshDrawableState();
        }
    }

    public static void clearError(boolean force, @NotNull TextInputLayout on, EditText editText, @ColorInt int color) {
        if (force || !TextUtils.isEmpty(on.getError())) {
            on.setError(null);
            editText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            on.refreshDrawableState();
        }
    }

    @NotNull
    public static Point getFixedViewSizeByDisplay(@NotNull Context context, @NotNull Point targetSize) {
        return getFixedViewSizeByDisplay(context, (float) targetSize.x / targetSize.y);
    }

    @NotNull
    public static Point getFixedViewSizeByDisplay(@NotNull Context context, float targetScale) {
        Display display = ((WindowManager) (context.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        Point screenSize = new Point(metrics.widthPixels, metrics.heightPixels);
        return getFixedViewSize(targetScale, screenSize);
    }

    @NotNull
    public static Point getFixedViewSize(@NotNull Point targetSize, @NotNull View view) {
        return getFixedViewSize(targetSize, new Point(view.getMeasuredWidth(), view.getMeasuredHeight()));
    }

    @NotNull
    public static Point getFixedViewSize(@NotNull Point targetSize, @Nullable Point measuredViewSize) {
        return getFixedViewSize(targetSize, measuredViewSize, null);
    }

    @NotNull
    public static Point getFixedViewSize(@NotNull Point targetSize, @Nullable Point measuredViewSize, @Nullable Point maxViewSize) {
        return getFixedViewSize((float) targetSize.x / targetSize.y, measuredViewSize, maxViewSize);
    }

    @NotNull
    public static Point getFixedViewSize(float targetScale, @Nullable Point measuredViewSize) {
        return getFixedViewSize(targetScale, measuredViewSize, null);
    }

    @NotNull
    public static Point getFixedViewSize(float targetScale, @Nullable Point measuredViewSize, @Nullable Point maxViewSize) {
        if (targetScale < 0) {
            throw new IllegalArgumentException("targetScale < 0");
        }

        Point newViewSize = new Point();

        if (targetScale == 0) {
            return newViewSize;
        }

        if (measuredViewSize == null) {
            measuredViewSize = new Point();
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

    @NotNull
    public static Point fixViewSize(Point targetSize, @Nullable View view) {
        return fixViewSize(targetSize, view, null);
    }

    @NotNull
    public static Point fixViewSize(float targetScale, @Nullable View view) {
        return fixViewSize(targetScale, view, null);
    }

    @NotNull
    public static Point fixViewSize(Point targetSize, @Nullable View view, @Nullable Point maxViewSize) {
        return fixViewSize(targetSize != null ? (float) targetSize.x / targetSize.y : 0, view, maxViewSize);
    }

    @NotNull
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

    @NotNull
    public static Point getAutoScaledSize(@NotNull View view, @Nullable Point maxViewSize, int fixedSize) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        width = width <= 0 ? (maxViewSize != null ? maxViewSize.x : 0) : width;
        height = height <= 0 ? (maxViewSize != null ? maxViewSize.y : 0) : height;
        return getAutoScaledSize(new Point(width, height), fixedSize);
    }

    @NotNull
    public static Point getAutoScaledSize(@NotNull Point size, int fixedSize) {
        return getAutoScaledSize((float) size.x / size.y, fixedSize);
    }

    @NotNull
    public static Point getAutoScaledSize(float scale, int fixedSize) {
        return getScaledSize(scale, fixedSize, scale > 1.0f);
    }

    @NotNull
    public static Point getScaledSize(@NotNull View view, @Nullable Point maxViewSize, int fixedSize, boolean isWidth) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        width = width <= 0 ? (maxViewSize != null ? maxViewSize.x : 0) : width;
        height = height <= 0 ? (maxViewSize != null ? maxViewSize.y : 0) : height;
        return getScaledSize(new Point(width, height), fixedSize, isWidth);
    }

    @NotNull
    public static Point getScaledSize(@NotNull Point size, int fixedSize, boolean isWidth) {
        if (size.x <= 0 || size.y <= 0) {
            throw new IllegalArgumentException("incorrect size: " + size.x + "x" + size.y);
        }
        return getScaledSize((float) size.x / size.y, fixedSize, isWidth);
    }

    @NotNull
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

    @NotNull
    public static androidx.core.util.Pair<Integer, Integer> calcAspectRatioFor(int width, int height) {
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
        androidx.core.util.Pair<Integer, Integer> result;
        if (width > height) {
            result = new androidx.core.util.Pair<>(dividend, divider);
        } else {
            result = new androidx.core.util.Pair<>(divider, dividend);
        }
        return result;
    }


    public static void setViewSize(@NotNull View view, @NotNull Point size) {
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
            behavior.onNestedFling(rootLayout, appbarLayout, coordinatorChild, 0, 10000, true);
        }
    }

    public static int getCurrentDisplayOrientation(@NotNull Context context) {
        final WindowManager windowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        if (windowManager == null) {
            throw new NullPointerException(WindowManager.class.getSimpleName() + " is null");
        }
        int degrees = 0;
        final int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
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

    public static int calculateViewsRotation(@NotNull Context context, int displayRotation) {
        int result = 0;
        displayRotation = getCorrectedDisplayRotation(displayRotation);
        if (displayRotation != ROTATION_NOT_SPECIFIED) {
            final int currentOrientation = context.getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                switch (displayRotation) {
                    case 0:
                        result = 270;
                        break;
                    case 90:
                        result = 180;
                        break;
                    case 180:
                        result = 90;
                        break;
                    case 270:
                        result = 0;
                }
            } else {
                switch (displayRotation) {
                    case 0:
                        result = 0;
                        break;
                    case 90:
                        result = 270;
                        break;
                    case 180:
                        result = 180;
                        break;
                    case 270:
                        result = 90;
                }
            }
        }
        return result;
    }

    public static void copyToClipboard(@NotNull Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            throw new NullPointerException(ClipboardManager.class.getSimpleName() + " is null");
        }
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    @NotNull
    public static DisplayMetrics getDisplayMetrics(@NotNull Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            throw new NullPointerException(WindowManager.class.getSimpleName() + "");
        }
        final DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics;
    }

    @NotNull
    public static DeviceType getScreenType(@NotNull Context context) {
        final DisplayMetrics outMetrics = getDisplayMetrics(context);
        final DeviceType deviceType;
        int shortSize = Math.min(outMetrics.heightPixels, outMetrics.widthPixels);
        int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi;
        if (shortSizeDp < 600) {
            // 0-599dp: "phone" UI with a separate status & navigation bar
            deviceType = DeviceType.PHONE;
        } else if (shortSizeDp < 720) {
            // 600-719dp: "phone" UI with modifications for larger screens
            deviceType = DeviceType.HYBRID;
        } else {
            // 720dp: "tablet" UI with a single combined status & navigation bar
            deviceType = DeviceType.TABLET;
        }
        return deviceType;
    }

    public static boolean isTabletUI(Context con) {
        return getScreenType(con) == DeviceType.TABLET;
    }


    public static boolean isEnterKeyPressed(KeyEvent event, int actionId) {
        return event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_NULL;
    }

    public static void showAboveLockscreen(@NotNull Window window, boolean wakeScreen) {
        toggleAboveLockscreen(window, wakeScreen, true);
    }

    public static void hideAboveLockscreen(@NotNull Window window, boolean wakeScreen) {
        toggleAboveLockscreen(window, wakeScreen, false);
    }

    private static void toggleAboveLockscreen(@NotNull Window window, boolean wakeScreen, boolean toggle) {
        int flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        if (wakeScreen) {
            flags |= WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        if (toggle) {
            window.addFlags(flags);
        } else {
            window.clearFlags(flags);
        }
    }

    public enum DeviceType {

        PHONE, HYBRID, TABLET;
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

        @NotNull
        final EditText et;

        final int linesLimit;

        public EditTextKeyLimiter(@NotNull EditText et, int linesLimit) {

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
                    int editTextRowCount = text.split(NEXT_LINE).length;

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

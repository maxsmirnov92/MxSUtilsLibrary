/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package ru.gokidgo.gui.views.size;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import net.maxsmr.commonutils.android.gui.GuiUtils;

public class SizeNotifierFrameLayout extends FrameLayout {

    private static final int SIZE_UNKNOWN = -1;

    private int previousKeyboardHeight = SIZE_UNKNOWN;
    private int keyboardHeight = previousKeyboardHeight;

    private SizeNotifierFrameLayoutDelegate delegate;

    public interface SizeNotifierFrameLayoutDelegate {
        void onSizeChanged(int keyboardHeight, boolean isWidthGreater);
    }

    public SizeNotifierFrameLayout(Context context) {
        this(context, null);
    }

    public SizeNotifierFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SizeNotifierFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }


    public int getKeyboardHeight() {
        if (keyboardHeight == SIZE_UNKNOWN || keyboardHeight == 0) {
            keyboardHeight = GuiUtils.getKeyboardHeight(getRootView(), this);
        }
        return keyboardHeight;
    }

    public void setDelegate(SizeNotifierFrameLayoutDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        notifyHeightChanged();
    }

    public void notifyHeightChanged() {
        if (delegate != null) {
            keyboardHeight = getKeyboardHeight();
            DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
            final boolean isWidthGreater = dm.widthPixels > dm.heightPixels;
            if (keyboardHeight != previousKeyboardHeight) {
                previousKeyboardHeight = keyboardHeight;
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (delegate != null) {
                            delegate.onSizeChanged(keyboardHeight, isWidthGreater);
                        }
                    }
                });
            }
        }
    }

}

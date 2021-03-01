package net.maxsmr.commonutils.gui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class BlinkLayout extends FrameLayout {

    public static final int DEFAULT_BLINK_DELAY = 500;

    private static final int MESSAGE_BLINK = 0x42;

    private final Handler mHandler  = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == MESSAGE_BLINK) {
                if (mBlink) {
                    mBlinkState = !mBlinkState;
                    if (mBlinkEnabled) {
                        makeBlink();
                    }
                }
                invalidate();
                return true;
            }
            return false;
        }
    });

    private boolean mBlink;
    private boolean mBlinkState;

    private boolean mBlinkEnabled = false;
    private int mBlinkDelay = DEFAULT_BLINK_DELAY;

    public BlinkLayout(Context context) {
        this(context, null);
    }

    public BlinkLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlinkLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBlinkEnabled(boolean blinkEnabled, int blinkDelay) {
        if (blinkDelay <= 0) {
            throw new IllegalArgumentException("incorrect blinkDelay: " + blinkDelay);
        }
        boolean changed = mBlinkEnabled != blinkEnabled || mBlinkDelay != blinkDelay;
        mBlinkEnabled = blinkEnabled;
        mBlinkDelay = blinkDelay;
        if (changed) {
            if (mBlinkEnabled) {
                makeBlink();
            } else {
                mHandler.removeMessages(MESSAGE_BLINK);
            }
        }
    }

    private void makeBlink() {
        Message message = mHandler.obtainMessage(MESSAGE_BLINK);
        mHandler.sendMessageDelayed(message, mBlinkDelay);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mBlink = true;
        mBlinkState = true;

        if (mBlinkEnabled) {
            makeBlink();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mBlink = false;
        mBlinkState = true;

        mHandler.removeMessages(MESSAGE_BLINK);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mBlinkState) {
            super.dispatchDraw(canvas);
        }
    }
}
package net.maxsmr.commonutils.android.gui.views.font;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.jetbrains.annotations.NotNull;
import android.text.TextUtils;
import android.util.AttributeSet;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.data.CompareUtils;

import java.io.Serializable;

/**
 * @author msmirnov
 */
public class TypewriterFontTextView extends FontTextView {

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.getCallback() == mScheduledCharacterAdderCallback) {
                mScheduledCharacterAdderCallback = null;
            } else if (msg.getCallback() == mScheduledResetCallback) {
                mScheduledResetCallback = null;
            }
            super.handleMessage(msg);
        }
    };

    private boolean mScheduledStart = false;

    private boolean mIsStarted = false;
    private int mIndex = 0;
    private long mStartTime;

    private CharSequence mScheduledText;
    private AnimateParams mAnimateParams;

    private Runnable mScheduledCharacterAdderCallback;
    private Runnable mScheduledResetCallback;

    private TextAnimationListener mTextAnimationListener;
    private TextAnimationTimeoutListener mTextAnimationTimeoutListener;

    public TypewriterFontTextView(Context context) {
        this(context, null);
    }

    public TypewriterFontTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TypewriterFontTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        obtainParamsFromAttrs(attrs);
    }

    private void obtainParamsFromAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TypewriterFontTextView, 0, 0);

            try {

                setScheduledText(a.getString(R.styleable.TypewriterFontTextView_scheduled_text));

                long timeLimit = a.getInteger(R.styleable.TypewriterFontTextView_text_anim_time_limit, 0);
                long delay = a.getInteger(R.styleable.TypewriterFontTextView_text_anim_delay, 0);
                boolean isLooping = a.getBoolean(R.styleable.TypewriterFontTextView_text_anim_looping, false);
                mScheduledStart = a.getBoolean(R.styleable.TypewriterFontTextView_text_anim_start, false);

                mAnimateParams = new AnimateParams(timeLimit, delay, isLooping);

            } finally {
                a.recycle();
            }
        }
    }

    public AnimateParams getAnimateParams() {
        return mAnimateParams;
    }

    public CharSequence getScheduledText() {
        return mScheduledText;
    }

    public void setScheduledText(CharSequence scheduledText) {
        synchronized (mHandler) {
            if (!CompareUtils.objectsEqual(mScheduledText, scheduledText)) {
                boolean isStarted = mIsStarted;
                if (isStarted) {
                    stopAnimateText();
                }
                this.mScheduledText = scheduledText;
                if (isStarted) {
                    startAnimateText(mAnimateParams);
                }
            }
        }
    }

    public void setTextAnimationListener(TextAnimationListener listener) {
        synchronized (mHandler) {
            this.mTextAnimationListener = listener;
        }
    }

    public void setTimeLimitExceededListener(TextAnimationTimeoutListener listener) {
        synchronized (mHandler) {
            this.mTextAnimationTimeoutListener = listener;
        }
    }

    public long getAnimationStartTime() {
        synchronized (mHandler) {
            return mStartTime;
        }
    }

    public boolean isAnimationStarted() {
        synchronized (mHandler) {
            return mIsStarted;
        }
    }

    public boolean isAnimationTimeouted() {

        synchronized (mHandler) {

            if (!mIsStarted) {
                return false;
            }

            long current = System.currentTimeMillis();
            return mAnimateParams.mTimeLimit != AnimateParams.NO_LIMIT && current - mStartTime >= mAnimateParams.mTimeLimit;
        }
    }

    public void startAnimateText(@NotNull AnimateParams animateParams) {

        synchronized (mHandler) {
            if (!mIsStarted) {
                restartAnimateText(animateParams);
            }
        }
    }

    public void restartAnimateText(@NotNull AnimateParams animateParams) {

        synchronized (mHandler) {

            stopAnimateText();

            if (TextUtils.isEmpty(mScheduledText)) {
                return;
            }

            mAnimateParams = animateParams;

            onTextAnimationStart();
        }
    }

    public void stopAnimateText() {
        synchronized (mHandler) {
            if (mIsStarted) {
                onTextAnimationEnd();
            }
        }
    }

    private void onTextAnimationStart() {
        mIndex = 0;
        mStartTime = System.currentTimeMillis();

        setText("");

        mHandler.postDelayed(mScheduledCharacterAdderCallback = new CharacterAdder(), mAnimateParams.mDelay);
        if (mAnimateParams.mTimeLimit != AnimateParams.NO_LIMIT) {
            mHandler.postDelayed(mScheduledResetCallback = new ResetRunnable(), mAnimateParams.mTimeLimit);
        }

        mIsStarted = true;

        if (mTextAnimationListener != null) {
            synchronized (mHandler) {
                mTextAnimationListener.onTextAnimationStart();
            }
        }
    }

    private void onTextAnimationEnd() {

        if (mScheduledCharacterAdderCallback != null) {
            mHandler.removeCallbacks(mScheduledCharacterAdderCallback);
            mScheduledCharacterAdderCallback = null;
        }
        if (mScheduledResetCallback != null) {
            mHandler.removeCallbacks(mScheduledResetCallback);
            mScheduledResetCallback = null;
        }

        mIndex = mScheduledText.length();
        setText(mScheduledText);
        mStartTime = 0;

        mIsStarted = false;

        if (mTextAnimationListener != null) {
            synchronized (mHandler) {
                mTextAnimationListener.onTextAnimationEnd();
            }
        }
    }

    private void onLimitExceeded() {

        stopAnimateText();

        if (mTextAnimationTimeoutListener != null) {
            mTextAnimationTimeoutListener.onTextAnimationTimeout();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mScheduledStart) {
            startAnimateText(mAnimateParams);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimateText();
    }

    final class CharacterAdder implements Runnable {

        @Override
        public void run() {
            synchronized (mHandler) {
                if (mIndex <= mScheduledText.length()) {
                } else if (mIndex == mScheduledText.length() + 1 && mAnimateParams.mIsLooping) {
                    mIndex = 0;
                } else {
                    mIsStarted = false;
                    return;
                }
                if (!isAnimationTimeouted()) {
                    if (mIndex == 0) {
                        mIndex++;
                    }
                    setText(mScheduledText.subSequence(0, mIndex++));
                    mHandler.postDelayed(mScheduledCharacterAdderCallback = new CharacterAdder(), mAnimateParams.mDelay);
                } else {
                    onLimitExceeded();
                }
            }
        }
    }

    final class ResetRunnable implements Runnable {

        @Override
        public void run() {
            synchronized (mHandler) {
                if (isAnimationTimeouted()) {
                    onLimitExceeded();
                }
            }
        }
    }

    public static class AnimateParams implements Serializable {

        public static final int NO_LIMIT = 0;

        public final long mTimeLimit;
        public final long mDelay;
        public final boolean mIsLooping;

        public AnimateParams(long timeLimit, long delay, boolean isLooping) {

            if (timeLimit < 0) {
                throw new IllegalArgumentException("incorrect timeLimit: " + timeLimit);
            }

            if (delay < 0) {
                throw new IllegalArgumentException("incorrect delay: " + delay);
            }

            mTimeLimit = timeLimit;
            mDelay = delay;
            mIsLooping = isLooping;
        }

        @Override
        public String toString() {
            return "AnimateParams{" +
                    "mTimeLimit=" + mTimeLimit +
                    ", mDelay=" + mDelay +
                    ", mIsLooping=" + mIsLooping +
                    '}';
        }
    }

    public interface TextAnimationListener {

        void onTextAnimationStart();

        void onTextAnimationEnd();
    }

    public interface TextAnimationTimeoutListener {

        void onTextAnimationTimeout();
    }
}

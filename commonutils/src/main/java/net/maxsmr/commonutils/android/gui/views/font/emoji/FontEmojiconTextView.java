package net.maxsmr.commonutils.android.gui.views.font.emoji;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.android.gui.fonts.FontsHolder;
import net.maxsmr.commonutils.android.gui.views.font.ITypefaceChangeable;
import net.maxsmr.commonutils.android.gui.views.font.OnSelectionChangedListener;

import java.util.LinkedHashSet;
import java.util.Set;

import io.github.rockerhieu.emojicon.EmojiconTextView;

public class FontEmojiconTextView extends EmojiconTextView implements ITypefaceChangeable {

    private HandlerThread thread;
    private Handler handler = new Handler(Looper.getMainLooper());

    private final Set<OnSelectionChangedListener> selectionChangedListeners = new LinkedHashSet<>();

    public void addOnSelectionChangedListener(@NonNull OnSelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    public void removeOnSelectionChangedListener(@NonNull OnSelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    public FontEmojiconTextView(Context context) {
        this(context, null);
    }

    public FontEmojiconTextView(Context context, AttributeSet attrs) {
        this(context, attrs, context.getResources().getIdentifier("editTextStyle", "attr", "android"));
    }

    public FontEmojiconTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            setTypefaceByName(getTypeFaceNameFromAttrs(attrs));
        }
    }

    private String getTypeFaceNameFromAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FontEditText, 0, 0);
            try {
                return a.getString(R.styleable.FontEditText_custom_typeface);
            } finally {
                a.recycle();
            }
        }
        return null;
    }

    public void setTypefaceByName(String name) {
        Typeface typeface = FontsHolder.getInstance().getFont(name);
        if (typeface != null) {
            setTypeface(typeface);
        }
    }


    private void prepareThread() {
        thread = new HandlerThread("EditSelectionChangeThread");
        thread.start();
    }

    @Override
    protected void onSelectionChanged(final int selStart, final int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (thread != null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (selectionChangedListeners) {
                        for (OnSelectionChangedListener l : selectionChangedListeners) {
                            l.onSelectionChanged(selStart, selEnd);
                        }
                    }
                }
            }, 0);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            prepareThread();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!isInEditMode()) {
            if (thread != null) {
                thread.quit();
                thread = null;
            }
        }
    }

}
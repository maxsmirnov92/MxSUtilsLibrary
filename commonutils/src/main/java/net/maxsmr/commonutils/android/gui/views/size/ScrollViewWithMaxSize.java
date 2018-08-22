package net.maxsmr.commonutils.android.gui.views.size;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ScrollView;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;


public class ScrollViewWithMaxSize extends ScrollView {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ScrollViewWithMaxSize.class);

    public static final int NOT_SET = -1;

    private int maxWidth = NOT_SET;
    private int maxHeight = NOT_SET;

    public ScrollViewWithMaxSize(Context context) {
        this(context, null);
    }

    public ScrollViewWithMaxSize(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollViewWithMaxSize(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initFromAttrs(attrs);
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxSize(int maxWidth, int maxHeight) {
        if ((maxWidth < 0 && maxWidth != NOT_SET) ||
                (maxHeight < 0 && maxHeight != NOT_SET)) {
            throw new IllegalArgumentException("incorrect size: " + maxWidth + "x" + maxHeight);
        }
        boolean changed = false;
        if (maxWidth != this.maxWidth) {
            this.maxWidth = maxWidth;
            changed = true;
        }
        if (maxHeight != this.maxHeight) {
            this.maxHeight = maxHeight;
            changed = true;
        }
        if (changed) {
            invalidate();
        }
    }

    private void initFromAttrs(AttributeSet attrs) {
        int maxWidth = NOT_SET;
        int maxHeight = NOT_SET;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ScrollViewMaxSize, 0, 0);
            try {
                maxWidth = a.getDimensionPixelSize(R.styleable.ScrollViewMaxSize_max_width, NOT_SET);
                maxHeight = a.getDimensionPixelSize(R.styleable.ScrollViewMaxSize_max_height, NOT_SET);
            } finally {
                a.recycle();
            }
        }
        setMaxSize(maxWidth, maxHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {

            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            if (maxWidth != NOT_SET) {
                if (widthSize > maxWidth) {
                    widthSize = maxWidth;
                }
                if (widthSize >= 0 && widthSize <= 1073741823) {
                    widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
                }
            }

            if (maxHeight != NOT_SET) {
                if (heightSize > maxHeight) {
                    heightSize = maxHeight;
                }
                if (heightSize >= 0 && heightSize <= 1073741823) {
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);
                }
            }

            setMeasuredDimension(widthSize, heightSize);

        } catch (Exception e) {
            logger.e(e);
        } finally {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}

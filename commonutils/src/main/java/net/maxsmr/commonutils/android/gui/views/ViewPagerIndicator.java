package net.maxsmr.commonutils.android.gui.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.graphic.GraphicUtils;

import org.jetbrains.annotations.Nullable;


/**
 * @author maxsmirnov
 */
public class ViewPagerIndicator extends LinearLayout implements ViewPager.OnPageChangeListener {

    private ViewPager viewPager;

    @Nullable
    private Drawable indicatorDrawable = null;

    private int indicatorMarginPx = 0;

    private boolean allowDisplaySingle = true;

    // TODO Animation

    public ViewPagerIndicator(Context context) {
        super(context);
        if (!isInEditMode()) {
            init(null);
        }
    }

    public ViewPagerIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            init(attrs);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public ViewPagerIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            init(attrs);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ViewPagerIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (!isInEditMode()) {
            init(attrs);
        }
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {

            Drawable indicatorDrawable = null;
            int indicatorMarginPx = 0;
            boolean allowDisplaySingle = false;

            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ViewPagerIndicator, 0, 0);
            try {
                indicatorDrawable = a.getDrawable(R.styleable.ViewPagerIndicator_indicatorBackground);
                indicatorMarginPx = a.getDimensionPixelSize(R.styleable.ViewPagerIndicator_marginSize, 0);
                allowDisplaySingle = a.getBoolean(R.styleable.ViewPagerIndicator_allowDisplaySingle, false);
            } finally {
                a.recycle();
            }

            setIndicatorDrawable(indicatorDrawable);
            if (indicatorMarginPx > 0) {
                setIndicatorMarginPx(indicatorMarginPx);
            }
            setAllowDisplaySingle(allowDisplaySingle);
        }
    }

    public void setIndicatorDrawableRes(@DrawableRes int indicatorDrawableId) {
        setIndicatorDrawable(ContextCompat.getDrawable(getContext(), indicatorDrawableId));
    }

    public void setIndicatorDrawable(@Nullable Drawable indicatorDrawable) {
        if (indicatorDrawable != this.indicatorDrawable) {
            if (indicatorDrawable == null) {
                throw new NullPointerException("drawable can't be null");
            }
            this.indicatorDrawable = indicatorDrawable;
            if (viewPager != null) {
                notifyDataSetChanged();
            }
        }
    }


    public void setIndicatorMarginDimenRes(@DimenRes int indicatorMarginResId) {
        setIndicatorMarginPx(getResources().getDimensionPixelSize(indicatorMarginResId));
    }

    public void setIndicatorMarginPx(int indicatorMarginPx) {
        if (indicatorMarginPx != this.indicatorMarginPx) {
            if (indicatorMarginPx < 0) {
                throw new IllegalArgumentException("incorrect indicatorMarginPx: " + indicatorMarginPx);
            }
            this.indicatorMarginPx = indicatorMarginPx;
            if (viewPager != null) {
                notifyDataSetChanged();
            }
        }
    }

    public void setAllowDisplaySingle(boolean toggle) {
        if (allowDisplaySingle != toggle) {
            allowDisplaySingle = toggle;
            if (viewPager != null) {
                notifyDataSetChanged();
            }
        }
    }

    public void setViewPager(@Nullable ViewPager viewPager) {
        if (viewPager != this.viewPager) {
            if (this.viewPager != null) {
                this.viewPager.removeOnPageChangeListener(this);
            }
            this.viewPager = viewPager;
            if (this.viewPager != null) {
                this.viewPager.addOnPageChangeListener(this);
                notifyDataSetChanged();
            } else {
                clear();
            }
        }
    }

    private void clear() {
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).setOnClickListener(null);
            }
        }
        removeAllViews();
    }

    private void populate() {

        if (viewPager == null) {
            throw new IllegalStateException(ViewPager.class.getSimpleName() + " is not specified");
        }

        PagerAdapter adapter = viewPager.getAdapter();

        if (adapter == null) {
            throw new IllegalStateException(PagerAdapter.class.getSimpleName() + "is not specified");
        }

        clear();

        final int adapterCount = adapter.getCount();
        if (adapterCount > 1 || allowDisplaySingle) {
            for (int i = 0; i < adapterCount; i++) {
                ImageView imageView = new ImageView(getContext());
                imageView.setImageDrawable(GraphicUtils.cloneDrawable(indicatorDrawable));
                LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int margin = indicatorMarginPx;
                lp.setMargins(margin, margin, margin, margin);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setLayoutParams(lp);
                final int itemPosition = i;
                imageView.setOnClickListener(v -> viewPager.setCurrentItem(itemPosition));
                addView(imageView, lp);
            }
        }
        invalidateByCurrentItem();
    }

    private void invalidateByCurrentItem() {

        if (viewPager == null) {
            throw new IllegalStateException(ViewPager.class.getSimpleName() + " is not specified");
        }

        PagerAdapter adapter = viewPager.getAdapter();

        if (adapter == null) {
            throw new IllegalStateException(PagerAdapter.class.getSimpleName() + "is not specified");
        }

        final int adapterCount = adapter.getCount();
        final int childCount = getChildCount();

        if (!allowDisplaySingle && adapterCount == 1 && childCount == 0) {
            return;
        }

        if (childCount != viewPager.getAdapter().getCount()) {
            throw new IllegalStateException("notifyDataSetChanged() was not called");
        }

        for (int i = 0; i < childCount; i++) {
            boolean selected = viewPager.getCurrentItem() == i;
            ImageView imageView = (ImageView) getChildAt(i);
            imageView.setSelected(selected);
        }
    }

    public void notifyDataSetChanged() {
        populate();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        invalidateByCurrentItem();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        setViewPager(null);
    }
}

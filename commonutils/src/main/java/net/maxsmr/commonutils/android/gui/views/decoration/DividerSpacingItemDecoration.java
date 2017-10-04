package net.maxsmr.commonutils.android.gui.views.decoration;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.maxsmr.commonutils.graphic.GraphicUtils;

public class DividerSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = new int[]{
            android.R.attr.listDivider
    };

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

    @NonNull
    protected DecorationSettings settings = new DecorationSettings();

    @Orientation
    protected int mOrientation = VERTICAL_LIST;

    protected boolean isReverse = false;

    @Nullable
    protected Drawable mDivider;

    protected int mSpace = 0;

    /**
     * Default divider will be used
     */
    public DividerSpacingItemDecoration(Context context, @Orientation int orientation) {
        final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
        setOrientation(orientation);
        setDivider(styledAttributes.getDrawable(0));
        styledAttributes.recycle();
    }

    /**
     * Custom divider will be used
     */
    public DividerSpacingItemDecoration(Context context, @Orientation int orientation, @DrawableRes int dividerResId, int space, boolean isReverse) {
        this(orientation, ContextCompat.getDrawable(context, dividerResId), space, isReverse);
    }

    /**
     * Custom divider will be used
     */
    public DividerSpacingItemDecoration(@Orientation int orientation, @Nullable Drawable divider, int space, boolean isReverse) {
        setOrientation(orientation);
        setDivider(divider);
        setSpace(space);
        setReverse(isReverse);
    }

    public void setReverse(boolean reverse) {
        isReverse = reverse;
    }

    @NonNull
    public DecorationSettings getSettings() {
        return settings;
    }

    public void setSettings(@NonNull DecorationSettings settings) {
        this.settings = settings;
    }

    public void setOrientation(@Orientation int orientation) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw new IllegalArgumentException("invalid orientation: " + orientation);
        }
        mOrientation = orientation;
    }

    @Nullable
    public Drawable getDivider() {
        return mDivider;
    }

    public void setDivider(@Nullable Drawable divider) {
        mDivider = divider;
    }

    public int getSpace() {
        return mSpace;
    }

    public void setSpace(int space) {
        if (space < 0) {
            throw new IllegalArgumentException("incorrect space: " + space);
        }
        this.mSpace = space;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }


    public void drawVertical(Canvas c, RecyclerView parent) {
        if (mDivider != null) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                if (isDecorated(child, parent, true)) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                            .getLayoutParams();
                    final int top;
                    final int bottom;
                    final int height = mDivider.getIntrinsicHeight() >= 0? mDivider.getIntrinsicHeight() : 0;
                    if (!isReverse) {
                        top = child.getBottom() + params.bottomMargin;
                        bottom = top + height;
                    } else {
                        bottom = child.getTop() + params.topMargin;
                        top = bottom + height;
                    }
                    mDivider.setBounds(left, top, right, bottom);
                    mDivider.draw(c);
                }
            }
        }
    }

    public void drawHorizontal(Canvas c, RecyclerView parent) {
        if (mDivider != null) {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getHeight() - parent.getPaddingBottom();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                if (isDecorated(child, parent, true)) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                            .getLayoutParams();
                    final int left;
                    final int right;
                    final int width = mDivider.getIntrinsicWidth() >= 0? mDivider.getIntrinsicWidth() : 0;
                    if (!isReverse) {
                        left = child.getRight() + params.rightMargin;
                        right = left + width;
                    } else {
                        right = child.getLeft() + params.leftMargin;
                        left = right + width;
                    }
                    mDivider.setBounds(left, top, right, bottom);
                    mDivider.draw(c);
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (isDecorated(view, parent, false)) {
            if (mDivider != null && mSpace == 0) {
                if (mOrientation == VERTICAL_LIST) {
                    outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
                } else {
                    outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
                }
            } else if (mSpace >= 0) {
                if (mOrientation == VERTICAL_LIST) {
                    if (!isReverse) {
                        outRect.bottom = mSpace;
                    } else {
                        outRect.top = mSpace;
                    }
                } else {
                    if (!isReverse) {
                        outRect.right = mSpace;
                    } else {
                        outRect.left = mSpace;
                    }
                }
            }
        }
    }

    @IntDef({HORIZONTAL_LIST, VERTICAL_LIST})
    @Retention(RetentionPolicy.SOURCE)
    @interface Orientation {

    }

    protected boolean isDecorated(View view, RecyclerView parent, boolean dividerOrSpacing) {

        int childPos = parent.getChildAdapterPosition(view);

        boolean result;

        switch (settings.getMode()) {
            case ALL:
                result = true;
                break;
            case ALL_EXCEPT_LAST:
                result = childPos < parent.getAdapter().getItemCount() - 1;
                break;
            case CUSTOM:
                result = dividerOrSpacing ? settings.getDividerPositions().contains(childPos) : (settings.getDividerPositions().contains(childPos) || settings.getSpacingPositions().contains(childPos));
                break;
            default:
                throw new IllegalArgumentException("incorrect " + DecorationSettings.Mode.class.getSimpleName() + ": " + settings.getMode());
        }

        return result;
    }

    public static class DecorationSettings {

        @NonNull
        private final Mode mode;

        @NonNull
        private final Set<Integer> dividerPositions = new LinkedHashSet<>();

        private final Set<Integer> spacingPositions = new LinkedHashSet<>();

        @NonNull
        public Mode getMode() {
            return mode;
        }

        @NonNull
        public Set<Integer> getDividerPositions() {
            return Collections.unmodifiableSet(dividerPositions);
        }

        public Set<Integer> getSpacingPositions() {
            return Collections.unmodifiableSet(spacingPositions);
        }

        public DecorationSettings() {
            this(Mode.ALL_EXCEPT_LAST);
        }

        public DecorationSettings(@NonNull Mode mode) {
            this(mode, null, null);
        }

        public DecorationSettings(@NonNull Mode mode, @Nullable Collection<Integer> dividerPositions, @Nullable Collection<Integer> spacingPositions) {
            this.mode = mode;
            if (dividerPositions != null) {
                this.dividerPositions.addAll(dividerPositions);
            }
            if (spacingPositions != null) {
                this.spacingPositions.addAll(spacingPositions);
            }
        }

        public enum Mode {
            ALL, ALL_EXCEPT_LAST, CUSTOM
        }
    }
}
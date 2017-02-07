package ru.gokidgo.gui.views.decoration;

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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


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
    public DividerSpacingItemDecoration(Context context, @Orientation int orientation, @DrawableRes int dividerResId, int space) {
        this(context, orientation, ContextCompat.getDrawable(context, dividerResId), space);
    }

    /**
     * Custom divider will be used
     */
    public DividerSpacingItemDecoration(Context context, @Orientation int orientation, @Nullable Drawable divider, int space) {
        setOrientation(orientation);
        setDivider(divider);
        setSpace(space);
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
                if (isDecorated(child, parent)) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                            .getLayoutParams();
                    final int top = child.getBottom() + params.bottomMargin;
                    final int bottom = top + mDivider.getIntrinsicHeight();
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
                if (isDecorated(child, parent)) {
                    final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                            .getLayoutParams();
                    final int left = child.getRight() + params.rightMargin;
                    final int right = left + mDivider.getIntrinsicHeight();
                    mDivider.setBounds(left, top, right, bottom);
                    mDivider.draw(c);
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (isDecorated(view, parent)) {
            if (mDivider != null && mSpace == 0) {
                if (mOrientation == VERTICAL_LIST) {
                    outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
                } else {
                    outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
                }
            } else if (mSpace >= 0) {
                if (mOrientation == VERTICAL_LIST) {
                    outRect.bottom = mSpace;
                } else {
                    outRect.right = mSpace;
                }
            }
        }
    }

    @IntDef({HORIZONTAL_LIST, VERTICAL_LIST})
    @Retention(RetentionPolicy.SOURCE)
    @interface Orientation {

    }

    protected boolean isDecorated(View view, RecyclerView parent) {

        int childPos = parent.getChildAdapterPosition(view);

        boolean result = false;

        switch (settings.getMode()) {
            case ALL:
                result = true;
                break;
            case ALL_EXCEPT_LAST:
                result = childPos < parent.getAdapter().getItemCount() - 1;
                break;
            case CUSTOM:
                result = settings.getPositions().contains(childPos);
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
        private final Set<Integer> positions = new LinkedHashSet<>();

        @NonNull
        public Mode getMode() {
            return mode;
        }

        @NonNull
        public Set<Integer> getPositions() {
            return Collections.unmodifiableSet(positions);
        }

        public DecorationSettings() {
            mode = Mode.ALL_EXCEPT_LAST;
        }

        public DecorationSettings(@NonNull Mode mode) {
            this.mode = mode;
        }

        public DecorationSettings(@NonNull Mode mode, @Nullable Set<Integer> positions) {
            this.mode = mode;
            if (positions != null) {
                this.positions.addAll(positions);
            }
        }

        public enum Mode {
            ALL, ALL_EXCEPT_LAST, CUSTOM;
        }
    }
}
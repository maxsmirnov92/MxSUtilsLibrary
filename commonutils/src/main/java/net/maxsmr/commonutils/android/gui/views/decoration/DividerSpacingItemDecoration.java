package net.maxsmr.commonutils.android.gui.views.decoration;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Orientation;
import android.view.View;

import net.maxsmr.commonutils.data.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DividerSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = new int[]{
            android.R.attr.listDivider
    };

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

    @NotNull
    protected DecorationSettings settings = new DecorationSettings();

    @NotNull
    protected Set<Integer> orientations = new HashSet<>();

    protected boolean isReverse = false;

    @Nullable
    protected Drawable divider;

    protected int space = 0;


    public DividerSpacingItemDecoration(Context context, @Orientation int orientation, @DrawableRes int dividerResId, int space, boolean isReverse) {
        this(Collections.singleton(orientation), dividerResId != 0 ? ContextCompat.getDrawable(context, dividerResId) : null, space, isReverse);
    }

    public DividerSpacingItemDecoration(@Orientation int orientation, @Nullable Drawable divider, int space, boolean isReverse) {
        this(Collections.singleton(orientation), divider, space, isReverse);
    }

    public DividerSpacingItemDecoration(Context context, List<Integer> orientations, @DrawableRes int dividerResId, int space, boolean isReverse) {
        this(orientations, dividerResId != 0? ContextCompat.getDrawable(context, dividerResId) : null, space, isReverse);
    }

    public DividerSpacingItemDecoration(@Nullable Collection<Integer> orientations, @Nullable Drawable divider, int space, boolean isReverse) {
        setOrientations(orientations);
        setDivider(divider);
        setSpace(space);
        setReverse(isReverse);
    }

    public DividerSpacingItemDecoration(Context context, @Nullable Collection<Integer> orientations, int space, boolean isReverse) {
        final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
        setDivider(styledAttributes.getDrawable(0));
        styledAttributes.recycle();
        setOrientations(orientations);
        setSpace(space);
        setReverse(isReverse);
    }

    public boolean isReverse() {
        return isReverse;
    }

    public void setReverse(boolean reverse) {
        isReverse = reverse;
    }

    @NotNull
    public DecorationSettings getSettings() {
        return settings;
    }

    public void setSettings(@NotNull DecorationSettings settings) {
        this.settings = settings;
    }

    public void setOrientations(Collection<Integer> orientations) {
        this.orientations.clear();
        if (orientations != null) {
            for (Integer orientation : orientations) {
                if (orientation != null) {
                    if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
                        throw new IllegalArgumentException("Incorrect orientation: " + orientation);
                    }
                    this.orientations.add(orientation);
                }
            }
        }
        if (this.orientations.isEmpty()) {
            throw new IllegalArgumentException("No orientations specified");
        }
    }

    @Nullable
    public Drawable getDivider() {
        return divider;
    }

    public void setDivider(@Nullable Drawable divider) {
        this.divider = divider;
    }

    public int getSpace() {
        return space;
    }

    public void setSpace(int space) {
        if (space < 0) {
            throw new IllegalArgumentException("incorrect space: " + space);
        }
        this.space = space;
    }

    @Override
    public void onDraw(@NotNull Canvas c, @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
        super.onDraw(c, parent, state);
        if (hasOrientation(VERTICAL_LIST)) {
            drawVertical(c, parent);
        }
        if (hasOrientation(HORIZONTAL_LIST)) {
            drawHorizontal(c, parent);
        }
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (isDecorated(view, parent, false)) {
            if (divider != null && space == 0) {
                if (hasOrientation(VERTICAL_LIST)) {
                    if (!isReverse) {
                        outRect.set(0, 0, 0, divider.getIntrinsicHeight());
                    } else {
                        outRect.set(0, divider.getIntrinsicHeight(), 0, 0);
                    }
                }
                if (hasOrientation(HORIZONTAL_LIST)) {
                    if (!isReverse) {
                        outRect.set(0, 0, divider.getIntrinsicWidth(), 0);
                    } else {
                        outRect.set(divider.getIntrinsicWidth(), 0, 0, 0);
                    }
                }
            } else if (space >= 0) {
                if (hasOrientation(VERTICAL_LIST)) {
                    if (!isReverse) {
                        outRect.set(0, 0, 0, space);
                    } else {
                        outRect.set(0, space, 0, 0);
                    }
                }
                if (hasOrientation(HORIZONTAL_LIST)) {
                    if (!isReverse) {
                        outRect.set(0, 0, space, 0);
                    } else {
                        outRect.set(space, 0, 0, 0);
                    }
                }
            }
        }
    }

    protected boolean isDecorated(View view, RecyclerView parent, boolean dividerOrSpacing) {

        int childPos = parent.getChildAdapterPosition(view);

        boolean result;

        switch (settings.getMode()) {
            case ALL:
                result = true;
                break;
            case ALL_EXCEPT_LAST:
                final RecyclerView.Adapter adapter = parent.getAdapter();
                if (adapter == null) {
                    throw new RuntimeException("Adapter not set");
                }
                result = childPos < adapter.getItemCount() - 1;
                break;
            case CUSTOM:
                result = dividerOrSpacing ? settings.getDividerPositions().contains(childPos) : (settings.getDividerPositions().contains(childPos) || settings.getSpacingPositions().contains(childPos));
                break;
            default:
                throw new IllegalArgumentException("incorrect " + DecorationSettings.Mode.class.getSimpleName() + ": " + settings.getMode());
        }

        return result;
    }

    private void drawVertical(Canvas c, RecyclerView parent) {
        if (divider != null) {
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
                    final int height = divider.getIntrinsicHeight() >= 0? divider.getIntrinsicHeight() : 0;
                    if (!isReverse) {
                        top = child.getBottom() + params.bottomMargin;
                        bottom = top + height;
                    } else {
                        bottom = child.getTop() + params.topMargin;
                        top = bottom + height;
                    }
                    divider.setBounds(left, top, right, bottom);
                    divider.draw(c);
                }
            }
        }
    }

    private void drawHorizontal(Canvas c, RecyclerView parent) {
        if (divider != null) {
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
                    final int width = divider.getIntrinsicWidth() >= 0? divider.getIntrinsicWidth() : 0;
                    if (!isReverse) {
                        left = child.getRight() + params.rightMargin;
                        right = left + width;
                    } else {
                        right = child.getLeft() + params.leftMargin;
                        left = right + width;
                    }
                    divider.setBounds(left, top, right, bottom);
                    divider.draw(c);
                }
            }
        }
    }

    private boolean hasOrientation(@Orientation int orientation) {
        return Predicate.Methods.contains(orientations, element -> element != null && element == orientation);
    }

    public static class DecorationSettings {

        @NotNull
        private final Mode mode;

        @NotNull
        private final Set<Integer> dividerPositions = new LinkedHashSet<>();

        private final Set<Integer> spacingPositions = new LinkedHashSet<>();

        @NotNull
        public Mode getMode() {
            return mode;
        }

        @NotNull
        public Set<Integer> getDividerPositions() {
            return Collections.unmodifiableSet(dividerPositions);
        }

        public Set<Integer> getSpacingPositions() {
            return Collections.unmodifiableSet(spacingPositions);
        }

        public DecorationSettings() {
            this(Mode.ALL_EXCEPT_LAST);
        }

        public DecorationSettings(@NotNull Mode mode) {
            this(mode, null, null);
        }

        public DecorationSettings(@NotNull Mode mode, @Nullable Collection<Integer> dividerPositions, @Nullable Collection<Integer> spacingPositions) {
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
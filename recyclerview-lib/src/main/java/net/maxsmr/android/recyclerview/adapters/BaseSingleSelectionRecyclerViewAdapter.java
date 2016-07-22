package net.maxsmr.android.recyclerview.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Checkable;

import com.bejibx.android.recyclerview.selection.SelectionHelper;

import java.util.Collection;
import java.util.Set;

public abstract class BaseSingleSelectionRecyclerViewAdapter<I, VH extends BaseRecyclerViewAdapter.ViewHolder> extends BaseRecyclerViewAdapter<I, VH> {

    @Nullable
    private Drawable defaultDrawable, selectionDrawable;

    public BaseSingleSelectionRecyclerViewAdapter(@NonNull Context context, @LayoutRes int itemLayoutId, @Nullable Collection<I> items) {
        this(context, itemLayoutId, items, null, null);
    }

    public BaseSingleSelectionRecyclerViewAdapter(Context context, @LayoutRes int itemLayoutId, @Nullable Collection<I> items, Drawable defaultDrawable, Drawable selectionDrawable) {
        super(context, itemLayoutId, items);
        if (defaultDrawable != null) {
            setDefaultDrawable(defaultDrawable);
        }
        if (selectionDrawable != null) {
            setSelectionDrawable(selectionDrawable);
        }
    }

    @NonNull
    public abstract Set<SelectionHelper.SelectMode> getSelectionModes(int position);

    @Nullable
    public Drawable getDefaultDrawable() {
        return defaultDrawable;
    }

    @CallSuper
    public void setDefaultDrawable(@Nullable Drawable defaultDrawable) {
        this.defaultDrawable = defaultDrawable;
        if (isNotifyOnChange())
            notifyDataSetChanged();
    }

    @Nullable
    public Drawable getSelectionDrawable() {
        return selectionDrawable;
    }

    @CallSuper
    public void setSelectionDrawable(@Nullable Drawable selectionDrawable) {
        this.selectionDrawable = selectionDrawable;
        if (isNotifyOnChange())
            notifyDataSetChanged();
    }

    protected boolean allowTogglingSelection() {
        return true;
    }

    @Override
    @CallSuper
    protected void processItem(@NonNull VH holder, @Nullable I item, int position) {
        super.processItem(holder, item, position);
        processSelection(holder, item, position);
    }

    private void processSelection(@NonNull VH holder, @Nullable final I item, final int position) {
        for (SelectionHelper.SelectMode mode : getSelectionModes(position)) {
            switch (mode) {
                case CLICK:
                    holder.itemView.setClickable(true);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (position == selection) {
                                if (allowTogglingSelection()) {
                                    toggleSelection(position, true);
                                }
                            } else {
                                setSelection(position, true);
                            }
                            if (allowSetClickListener()) {
                                if (itemClickListener != null) {
                                    itemClickListener.onItemClick(position, item);
                                }
                            }
                        }
                    });
                    break;

                case LONG_CLICK:
                    holder.itemView.setLongClickable(true);
                    holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (position == selection) {
                                if (allowTogglingSelection()) {
                                    toggleSelection(position, true);
                                }
                            } else {
                                setSelection(position, true);
                            }
                            if (allowSetLongClickListener()) {
                                if (itemLongClickListener != null) {
                                    itemLongClickListener.onItemLongClick(position, item);
                                }
                            }
                            return true;
                        }
                    });
                    break;
            }
        }
        boolean isItemSelected = isItemPositionSelected(position);
        if (holder.itemView instanceof Checkable) {
            ((Checkable) holder.itemView).setChecked(isItemSelected);
        }
        if (isItemSelected) {
            onProcessItemSelected(holder);
        } else {
            onProcessItemNotSelected(holder);
        }
    }

    protected void onProcessItemSelected(@NonNull VH holder) {

    }

    protected void onProcessItemNotSelected(@NonNull VH holder) {

    }

    private int selection = RecyclerView.NO_POSITION;

    public boolean isSelected() {
        return selection != RecyclerView.NO_POSITION;
    }

    public int getSelectedPosition() {
        return selection;
    }

    public I getSelectedItem() {
        return getItem(selection);
    }

    public boolean isItemPositionSelected(int position) {
        rangeCheck(position);
        return selection != RecyclerView.NO_POSITION && selection == position;
    }

    public boolean isItemSelected(I item) {
        return isItemPositionSelected(indexOf(item));
    }

    public void setSelectionByItem(I item) {
        setSelection(indexOf(item));
    }

    public void setSelection(int selection) {
        setSelection(selection, false);
    }

    protected void setSelection(int selection, boolean fromUser) {
        rangeCheck(selection);
        if (this.selection != selection) {
            int previousSelection = this.selection;
            this.selection = selection;
            onSelectionChanged(previousSelection, this.selection, fromUser);
            if (isNotifyOnChange()) {
                notifyItemChanged(previousSelection);
                notifyItemChanged(selection);
            }
        }
    }

    public void resetSelection() {
        resetSelection(false);
    }

    private void resetSelection(boolean fromUser) {
        if (isSelected()) {
            int previousSelection = this.selection;
            this.selection = RecyclerView.NO_POSITION;
            onSelectionChanged(previousSelection, this.selection, fromUser);
            if (isNotifyOnChange()) {
                notifyItemChanged(previousSelection);
            }
        }
    }

    public void toggleSelection(int selection) {
        toggleSelection(selection, false);
    }

    private void toggleSelection(int selection, boolean fromUser) {
        if (isSelected()) {
            resetSelection(fromUser);
        } else {
            setSelection(selection, fromUser);
        }
    }

    /**
     * called before {@link #notifyItemChanged(int)}}
     */
    @CallSuper
    protected void onSelectionChanged(int from, int to, boolean fromUser) {
        if (selectedChangeListener != null) {
            if (selection != RecyclerView.NO_POSITION) {
                selectedChangeListener.onSetSelection(from, to, fromUser);
            } else {
                selectedChangeListener.onResetSelection(from, fromUser);
            }
        }
    }

    @Nullable
    private OnSelectedChangeListener selectedChangeListener;

    public void setOnSelectedChangeListener(@Nullable OnSelectedChangeListener selectedChangeListener) {
        this.selectedChangeListener = selectedChangeListener;
    }

    public interface OnSelectedChangeListener {

        void onSetSelection(int fromIndex, int toIndex, boolean fromUser);

        void onResetSelection(int onIndex, boolean fromUser);
    }
}

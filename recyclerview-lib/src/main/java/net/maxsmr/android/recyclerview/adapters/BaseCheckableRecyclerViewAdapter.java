package net.maxsmr.android.recyclerview.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.widget.Checkable;

import com.bejibx.android.recyclerview.selection.HolderClickObserver;
import com.bejibx.android.recyclerview.selection.SelectionHelper;
import com.bejibx.android.recyclerview.selection.SelectionObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseCheckableRecyclerViewAdapter<I, VH extends BaseRecyclerViewAdapter.ViewHolder> extends BaseRecyclerViewAdapter<I, VH> implements HolderClickObserver, SelectionObserver {

    private SelectionHelper mSelectionHelper;

    @Nullable
    private Drawable defaultDrawable, selectionDrawable;

    public BaseCheckableRecyclerViewAdapter(@NonNull Context context, @LayoutRes int itemLayoutId, @Nullable Collection<I> items) {
        this(context, itemLayoutId, items, null, null, true);
    }

    public BaseCheckableRecyclerViewAdapter(@NonNull Context context, @LayoutRes int itemLayoutId, @Nullable Collection<I> items, @Nullable Drawable defaultDrawable, @Nullable Drawable selectionDrawable, boolean selectable) {
        super(context, itemLayoutId, items);
        initSelectionHelper();
        setSelectable(selectable);
        if (defaultDrawable != null) {
            setDefaultDrawable(defaultDrawable);
        }
        if (selectionDrawable != null) {
            setSelectionDrawable(selectionDrawable);
        }
    }

    private void initSelectionHelper() {
        if (mSelectionHelper == null) {
            mSelectionHelper = new SelectionHelper();
            mSelectionHelper.registerSelectionObserver(this);
            mSelectionHelper.registerHolderClickObserver(this);
        }
    }

    //    protected final SelectionHelper getSelectionHelper() {
//        return mSelectionHelper;
//    }

    public boolean isSelectable() {
        return mSelectionHelper.isSelectable();
    }

    public void setSelectable(boolean toggle) {
        mSelectionHelper.setSelectable(toggle);
    }

    @NonNull
    public abstract Set<SelectionHelper.SelectMode> getSelectionModes(int position);

    private void processSelection(@NonNull VH holder, @Nullable I item, int position) {
        mSelectionHelper.wrapSelectable(holder, getSelectionModes(position));

        final boolean isSelected = isItemSelected(position);

        if ((holder.itemView instanceof Checkable)) {
            ((Checkable) holder.itemView).setChecked(isSelected);
        }

        if (isSelected) {
            onProcessItemSelected(holder);
        } else {
            onProcessItemNotSelected(holder);
        }
    }

    @Override
    @CallSuper
    protected void processItem(@NonNull VH holder, @Nullable I item, int position) {
        super.processItem(holder, item, position);
        processSelection(holder, item, position);
    }

    @Override
    protected final boolean allowSetClickListener() {
        return false;
    }

    @Override
    protected final boolean allowSetLongClickListener() {
        return false;
    }

    @Nullable
    public Drawable getDefaultDrawable() {
        return defaultDrawable;
    }

    public void setDefaultDrawable(@Nullable Drawable defaultDrawable) {
        this.defaultDrawable = defaultDrawable;
        if (isNotifyOnChange())
            notifyDataSetChanged();
    }

    @Nullable
    public Drawable getSelectionDrawable() {
        return selectionDrawable;
    }

    public void setSelectionDrawable(@Nullable Drawable selectionDrawable) {
        this.selectionDrawable = selectionDrawable;
        if (isNotifyOnChange())
            notifyDataSetChanged();
    }

    @Override
    protected void onItemsSet() {
        super.onItemsSet();
        if (mSelectionHelper != null) {
            clearSelection();
        }
    }

    @Override
    protected void onItemsCleared() {
        super.onItemsCleared();
        if (mSelectionHelper != null) {
            clearSelection();
        }
    }

    @Override
    protected void onItemRemoved(int removedPosition, @Nullable I item) {
        super.onItemRemoved(removedPosition, item);
        if (mSelectionHelper != null) {
            if (mSelectionHelper.isItemSelected(removedPosition)) {
                mSelectionHelper.setItemSelectedByPosition(removedPosition, false);
            }
        }
    }

    @NonNull
    public List<I> getSelectedItems() {
        List<I> selectedItems = new ArrayList<>();
        LinkedHashSet<Integer> selectedPositions = getSelectedItemsPositions();
        for (Integer pos : selectedPositions) {
            selectedItems.add(getItem(pos));
        }
        return selectedItems;
    }

    @NonNull
    public LinkedHashSet<I> getUnselectedItems() {
        LinkedHashSet<I> unselectedItems = new LinkedHashSet<>();
        LinkedHashSet<Integer> unselectedPositions = getUnselectedItemsPositions();
        for (Integer pos : unselectedPositions) {
            unselectedItems.add(getItem(pos));
        }
        return unselectedItems;
    }

    @NonNull
    public LinkedHashSet<Integer> getSelectedItemsPositions() {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        return mSelectionHelper.getSelectedItems();
    }

    @NonNull
    public LinkedHashSet<Integer> getUnselectedItemsPositions() {
        LinkedHashSet<Integer> unselectedPositions = new LinkedHashSet<>();
        LinkedHashSet<Integer> selectedPositions = getSelectedItemsPositions();
        for (int pos = 0; pos < getItemCount(); pos++) {
            if (!selectedPositions.contains(pos)) {
                unselectedPositions.add(pos);
            }
        }
        return unselectedPositions;
    }

    public boolean isItemSelected(int position) {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        rangeCheck(position);
        return mSelectionHelper.isItemSelected(position);
    }

    public boolean isItemSelected(I item) {
        return isItemSelected(indexOf(item));
    }

    public int getSelectedItemsCount() {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        return getItemCount() > 0? mSelectionHelper.getSelectedItemsCount() : 0;
    }

    public boolean setItemsSelectedByPositions(@Nullable Collection<Integer> positions, boolean isSelected) {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        if (positions != null) {
            for (int pos : positions) {
                rangeCheck(pos);
            }
        }
        return mSelectionHelper.setItemsSelectedByPositions(positions, isSelected);
    }

    public boolean setItemSelectedByPosition(int position, boolean isSelected) {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        rangeCheck(position);
        return mSelectionHelper.setItemSelectedByPosition(position, isSelected);
    }

    public boolean toggleItemsSelectedByPositions(@Nullable Collection<Integer> positions) {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        if (positions != null) {
            for (int pos : positions) {
                rangeCheck(pos);
            }
        }
        return mSelectionHelper.toggleItemsSelectedByPositions(positions);
    }

    public boolean toggleItemSelectedByPosition(int position) {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        rangeCheck(position);
        return mSelectionHelper.toggleItemSelectedByPosition(position);
    }

    public boolean setItemsSelected(@Nullable Collection<I> items, boolean isSelected) {
        List<Integer> positions = new ArrayList<>();
        if (items != null) {
            for (I item : items) {
                int index = indexOf(item);
                if (index > -1) {
                    positions.add(index);
                }
            }
        }
        return setItemsSelectedByPositions(positions, isSelected);
    }

    public boolean setItemSelected(I item, boolean isSelected) {
        return setItemsSelected(Collections.singletonList(item), isSelected);
    }

    public boolean toggleItemsSelected(Collection<I> items) {
        List<Integer> positions = new ArrayList<>();
        if (items != null) {
            for (I item : items) {
                int index = indexOf(item);
                if (index > -1) {
                    positions.add(index);
                }
            }
        }
        return toggleItemsSelectedByPositions(positions);
    }

    public boolean toggleItemSelected(I item) {
        return toggleItemsSelected(Collections.singletonList(item));
    }

    public void clearSelection() {
        if (mSelectionHelper == null) {
            throw new IllegalStateException(SelectionHelper.class.getSimpleName() + " was not initialized");
        }
        if (mSelectionHelper.getSelectedItemsCount() > 0) {
            mSelectionHelper.clearSelection();
        }
    }

    protected void onProcessItemSelected(@NonNull VH holder) {

    }

    protected void onProcessItemNotSelected(@NonNull VH holder) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public final void onSelectedChanged(RecyclerView.ViewHolder holder, boolean isSelected) {
        if (itemSelectedChangeListener != null) {
            itemSelectedChangeListener.onItemSelectedChange(holder.getAdapterPosition(), isSelected);
        }
        if (isNotifyOnChange())
            notifyItemChanged(holder.getAdapterPosition());
    }

    @Override
    public void onSelectableChanged(boolean isSelectable) {

    }

    @Override
    public void onHolderClick(RecyclerView.ViewHolder holder) {

    }

    @Override
    public boolean onHolderLongClick(RecyclerView.ViewHolder holder) {
        return false;
    }


    @Nullable
    private OnItemSelectedChangeListener itemSelectedChangeListener;

    public void setOnSelectedChangeListener(@Nullable OnItemSelectedChangeListener selectedChangeListener) {
        this.itemSelectedChangeListener = selectedChangeListener;
    }

    public interface OnItemSelectedChangeListener {
        void onItemSelectedChange(int position, boolean isSelected);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (mSelectionHelper != null) {
            mSelectionHelper.unregisterSelectionObserver(this);
            mSelectionHelper.unregisterHolderClickObserver(this);
            mSelectionHelper = null;
        }
    }
}

package com.bejibx.android.recyclerview.selection;

import android.database.Observable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SelectionHelper {

    private final HolderWrapperTracker mTracker = new HolderWrapperTracker();
    private final LinkedHashSet<Integer> mSelectedItems = new LinkedHashSet<>();

    private final HolderClickObservable mHolderClickObservable = new HolderClickObservable();
    private final SelectionObservable mSelectionObservable = new SelectionObservable();

    private boolean mIsSelectable = false;

    public boolean isSelectable() {
        return mIsSelectable;

    }
    public void setSelectable(boolean isSelectable) {
        if (mIsSelectable != isSelectable) {
            mIsSelectable = isSelectable;
            if (!isSelectable) clearSelection();
            mSelectionObservable.notifySelectableChanged(isSelectable);
        }
    }

    public <H extends RecyclerView.ViewHolder> H wrapSelectable(@NonNull H holder, @NonNull Set<SelectMode> selectModes) {
        bindWrapper(new ViewHolderMultiSelectionWrapper(holder, selectModes));
        return holder;
    }

    public <H extends RecyclerView.ViewHolder> H wrapClickable(@NonNull H holder) {
        bindWrapper(new ViewHolderClickWrapper(holder));
        return holder;
    }

    private <W extends SelectionHelper.ViewHolderWrapper> void bindWrapper(@NonNull W wrapper) {
        RecyclerView.ViewHolder holder = wrapper.getHolder();
        if (holder != null)
            mTracker.bindWrapper(wrapper, holder.getAdapterPosition());
    }

    public boolean setItemSelectedByHolder(@NonNull RecyclerView.ViewHolder holder, boolean isSelected) {
        return setItemSelectedByPosition(holder.getAdapterPosition(), isSelected);
    }

    public boolean setItemsSelectedByHolders(@Nullable Collection<RecyclerView.ViewHolder> holders, boolean isSelected) {
        boolean success = false;
        if (holders != null) {
            success = true;
            for (RecyclerView.ViewHolder holder : holders) {
                if (holder == null || !setItemSelectedByHolder(holder, isSelected)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public boolean toggleItemSelectedByHolder(@NonNull RecyclerView.ViewHolder holder) {
        return setItemSelectedByHolder(holder, !isItemSelected(holder));
    }

    public boolean toggleItemsSelectedByHolders(@Nullable Collection<RecyclerView.ViewHolder> holders) {
        boolean success = true;
        if (holders != null) {
            for (RecyclerView.ViewHolder holder : holders) {
                if (!toggleItemSelectedByHolder(holder)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public boolean setItemSelectedByPosition(int position, boolean isSelected) {
        if (isSelectable() && position != RecyclerView.NO_POSITION) {
            boolean isAlreadySelected = isItemSelected(position);
            if (isSelected) {
                mSelectedItems.add(position);
            } else {
                mSelectedItems.remove(position);
            }
            if (isSelected ^ isAlreadySelected) {
                ViewHolderWrapper wrapper = mTracker.getWrapper(position);
                if (wrapper != null) {
                    mSelectionObservable.notifySelectionChanged(wrapper.getHolder(), isSelected);
                }
            }
            return true;
        }
        return false;
    }

    public boolean setItemsSelectedByPositions(@Nullable Collection<Integer> positions, boolean isSelected) {
        boolean success = false;
        if (positions != null) {
            success = true;
            for (int pos : positions) {
                if (!setItemSelectedByPosition(pos, isSelected)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public boolean toggleItemSelectedByPosition(int position) {
        return setItemSelectedByPosition(position, !isItemSelected(position));
    }

    public boolean toggleItemsSelectedByPositions(@Nullable Collection<Integer> positions) {
        boolean success = true;
        if (positions != null) {
            for (int pos : positions) {
                if (!toggleItemSelectedByPosition(pos)) {
                    success = false;
                }
            }
        }
        return success;
    }

    public void clearSelection() {
        if (!mSelectedItems.isEmpty()) {
            mSelectedItems.clear();
            for (ViewHolderWrapper wrapper : mTracker.getTrackedWrappers()) {
                if (wrapper != null) {
                    mSelectionObservable.notifySelectionChanged(wrapper.getHolder(), false);
                }
            }
        }
    }

    public LinkedHashSet<Integer> getSelectedItems() {
        return new LinkedHashSet<>(mSelectedItems);
    }

    public boolean isItemSelected(RecyclerView.ViewHolder holder) {
        return mSelectedItems.contains(holder.getAdapterPosition());
    }

    public boolean isItemSelected(int position) {
        return mSelectedItems.contains(position);
    }

    public int getSelectedItemsCount() {
        return mSelectedItems.size();
    }

    public final void registerHolderClickObserver(@NonNull HolderClickObserver observer) {
        mHolderClickObservable.registerObserver(observer);
    }

    public final void unregisterSelectionObserver(@NonNull SelectionObserver observer) {
        mSelectionObservable.unregisterObserver(observer);
    }

    public final void registerSelectionObserver(@NonNull SelectionObserver observer) {
        mSelectionObservable.registerObserver(observer);
    }

    public final void unregisterHolderClickObserver(@NonNull HolderClickObserver observer) {
        mHolderClickObservable.unregisterObserver(observer);
    }

    private class HolderClickObservable extends Observable<HolderClickObserver> {
        public final void notifyOnHolderClick(RecyclerView.ViewHolder holder) {
            synchronized (mObservers) {
                for (HolderClickObserver observer : mObservers) {
                    observer.onHolderClick(holder);
                }
            }
        }

        public final boolean notifyOnHolderLongClick(RecyclerView.ViewHolder holder) {
            boolean isConsumed = false;
            synchronized (mObservers) {
                for (HolderClickObserver observer : mObservers) {
                    isConsumed = isConsumed || observer.onHolderLongClick(holder);
                }
            }
            return isConsumed;
        }
    }

    private class SelectionObservable extends Observable<SelectionObserver> {
        private void notifySelectionChanged(RecyclerView.ViewHolder holder, boolean isSelected) {
            synchronized (mObservers) {
                for (SelectionObserver observer : mObservers) {
                    observer.onSelectedChanged(holder, isSelected);
                }
            }
        }

        private void notifySelectableChanged(boolean isSelectable) {
            synchronized (mObservers) {
                for (SelectionObserver observer : mObservers) {
                    observer.onSelectableChanged(isSelectable);
                }
            }
        }
    }

    abstract class ViewHolderWrapper implements android.view.View.OnClickListener {
        protected final WeakReference<RecyclerView.ViewHolder> mWrappedHolderRef;

        protected ViewHolderWrapper(RecyclerView.ViewHolder holder) {
            mWrappedHolderRef = new WeakReference<>(holder);
        }

        @Nullable
        public RecyclerView.ViewHolder getHolder() {
            return mWrappedHolderRef.get();
        }
    }

    public enum SelectMode {
        CLICK, LONG_CLICK
    }

    class ViewHolderMultiSelectionWrapper extends ViewHolderWrapper
            implements View.OnLongClickListener {

        private ViewHolderMultiSelectionWrapper(RecyclerView.ViewHolder holder, Set<SelectMode> selectModes) {
            super(holder);

            View itemView = holder.itemView;

            if (selectModes != null && !selectModes.isEmpty()) {

                for (SelectMode mode : selectModes) {
                    if (mode != null) {
                        switch (mode) {
                            case CLICK:
                                itemView.setClickable(true);
                                itemView.setOnClickListener(this);
                                break;
                            case LONG_CLICK:
                                itemView.setLongClickable(true);
                                itemView.setOnLongClickListener(this);
                                break;
                        }
                    }
                }
            }
        }

        @Override
        public final void onClick(View v) {
            RecyclerView.ViewHolder holder = mWrappedHolderRef.get();
            if (holder != null) {
                if (isSelectable()) {
                    toggleItemSelectedByHolder(holder);
                } else {
                    mHolderClickObservable.notifyOnHolderClick(holder);
                }
            }
        }

        @Override
        public final boolean onLongClick(View v) {
            RecyclerView.ViewHolder holder = mWrappedHolderRef.get();
            if (holder != null) {
                if (isSelectable()) {
                    toggleItemSelectedByHolder(holder);
                    return true;
                } else {
                    return mHolderClickObservable.notifyOnHolderLongClick(holder);
                }
            }
            return true;
        }
    }

    class ViewHolderClickWrapper extends ViewHolderWrapper {

        private ViewHolderClickWrapper(RecyclerView.ViewHolder holder) {
            super(holder);
            View itemView = holder.itemView;
            itemView.setOnClickListener(this);
            itemView.setClickable(true);
        }

        @Override
        public final void onClick(View v) {
            RecyclerView.ViewHolder holder = mWrappedHolderRef.get();
            if (holder != null) {
                mHolderClickObservable.notifyOnHolderClick(holder);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mTracker.clear();
        mSelectedItems.clear();
    }
}

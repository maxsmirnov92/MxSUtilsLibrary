package net.maxsmr.android.recyclerview.adapters;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class BaseRecyclerViewAdapter<I, VH extends BaseRecyclerViewAdapter.ViewHolder> extends RecyclerView.Adapter<VH> {

    @NonNull
    protected final Context mContext;

    @LayoutRes
    protected final int mBaseItemLayoutId;

    protected BaseRecyclerViewAdapter(@NonNull Context context, @LayoutRes int baseItemLayoutId, @Nullable Collection<I> items) {
        this.mContext = context;
        this.mBaseItemLayoutId = baseItemLayoutId;
        this.setItems(items);
    }

    @NonNull
    private final ArrayList<I> mItems = new ArrayList<>();

    protected final void rangeCheck(int position) {
        synchronized (mItems) {
            if (position < 0 || position >= mItems.size()) {
                throw new IndexOutOfBoundsException("incorrect position: " + position);
            }
        }
    }

    protected final void rangeCheckForAdd(int position) {
        synchronized (mItems) {
            if (position < 0 || position > mItems.size()) {
                throw new IndexOutOfBoundsException("incorrect add position: " + position);
            }
        }
    }

    @NonNull
    public final ArrayList<I> getItems() {
        synchronized (mItems) {
            return new ArrayList<>(mItems);
        }
    }

    @Nullable
    public final I getItem(int at) throws IndexOutOfBoundsException {
        synchronized (mItems) {
            rangeCheck(at);
            return mItems.get(at);
        }
    }

    public final int indexOf(I item) {
        synchronized (mItems) {
            return mItems.indexOf(item);
        }
    }

    public final int lastIndexOf(I item) {
        synchronized (mItems) {
            return mItems.lastIndexOf(item);
        }
    }

    public void sort(@NonNull Comparator<? super I> comparator) {
        synchronized (mItems) {
            Collections.sort(mItems, comparator);
            if (notifyOnChange) notifyDataSetChanged();
        }
    }

    /**
     * @param items null for reset adapter
     */
    public final void setItems(@Nullable Collection<I> items) {
        synchronized (mItems) {
            clearItems();
            if (items != null) {
                this.mItems.addAll(items);
            }
            onItemsSet();
            if (notifyOnChange)
                notifyDataSetChanged();
        }
    }

    protected void onItemsSet() {

    }

    public final void clearItems() {
        synchronized (mItems) {
            if (!isEmpty()) {
                int previousSize = getItemCount();
                mItems.clear();
                onItemsCleared();
                if (notifyOnChange)
                    notifyItemRangeRemoved(0, previousSize);
            }
        }
    }

    protected void onItemsCleared() {

    }

    public final void addItem(int to, @Nullable I item) throws IndexOutOfBoundsException {
        synchronized (mItems) {
            rangeCheckForAdd(to);
            mItems.add(to, item);
            onItemAdded(to, item);
            if (notifyOnChange)
                notifyItemRangeInserted(to, 1);
        }
    }

    public final void addItem(@Nullable I item) {
        addItem(getItemCount(), item);
    }

    public final void addItems(@NonNull List<I> items) {
        for (I item : items) {
            addItem(item);
        }
    }

    @CallSuper
    protected void onItemAdded(int addedPosition, @Nullable I item) {
        if (itemAddedListener != null) {
            itemAddedListener.onItemAdded(addedPosition, item);
        }
    }

    public final void setItem(int in, @Nullable I item) {
        synchronized (mItems) {
            rangeCheck(in);
            mItems.set(in, item);
            onItemSet(in, item);
            if (notifyOnChange)
                notifyItemChanged(in);
        }
    }

    @CallSuper
    protected void onItemSet(int setPosition, @Nullable I item) {
        if (itemSetListener != null) {
            itemSetListener.onItemSet(setPosition, item);
        }
    }

    @Nullable
    public final I removeItem(@Nullable I item) {
        return removeItem(indexOf(item));
    }

    public final I removeItem(int from) {
        synchronized (mItems) {
            rangeCheck(from);
            I removedItem = getItem(from);
            mItems.remove(from);
            onItemRemoved(from, removedItem);
            return removedItem;
        }
    }

    @NonNull
    public final List<I> removeItemsRange(int from, int to) {
        rangeCheck(from);
        rangeCheck(to);
        List<I> removed = new ArrayList<>();
        for (int pos = from; pos <= to; pos++) {
            removed.add(removeItem(pos));
        }
        return removed;
    }

    public final void removeAllItems() {
        synchronized (mItems) {
            for (I item : mItems) {
                removeItem(item);
            }
        }
    }

    @CallSuper
    protected void onItemRemoved(int removedPosition, @Nullable I item) {
        if (itemRemovedListener != null) {
            itemRemovedListener.onItemRemoved(removedPosition, item);
        }
    }

    protected final View onInflateView(ViewGroup parent, int viewType) {
        return LayoutInflater.from(parent.getContext())
                .inflate(getLayoutIdForViewType(viewType), parent, false);
    }

    @LayoutRes
    protected int getLayoutIdForViewType(int viewType) {
        return mBaseItemLayoutId;
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public final void onBindViewHolder(VH holder, int position) {
        final I item = (position >= 0 && position < mItems.size()) ? mItems.get(position) : null;
        processItem(holder, item, position);
    }

    protected boolean allowSetClickListener() {
        return true;
    }

    protected boolean allowSetLongClickListener() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @CallSuper
    protected void processItem(@NonNull VH holder, @Nullable final I item, final int position) {

        if (processingItemListener != null) {
            processingItemListener.onProcessingItem(holder, item, position);
        }

        if (item != null) {

            if (allowSetClickListener()) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (itemClickListener != null) {
                            itemClickListener.onItemClick(position, item);
                        }
                    }
                });
            }

            if (allowSetLongClickListener()) {
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        boolean consumed = false;
                        if (itemLongClickListener != null) {
                            consumed = itemLongClickListener.onItemLongClick(position, item);
                        }
                        return consumed;
                    }
                });
            }

            holder.displayData(position, item);

        } else {
            holder.displayNoData(position, null);
        }
    }

    public final boolean isEmpty() {
        return getItemCount() == 0;
    }

    @Override
    public final int getItemCount() {
        return mItems.size();
    }

    private boolean notifyOnChange = true;

    public final boolean isNotifyOnChange() {
        return notifyOnChange;
    }

    public void setNotifyOnChange(boolean enable) {
        notifyOnChange = enable;
    }

    public interface OnItemClickListener<I> {
        void onItemClick(int position, I item);
    }

    protected OnItemClickListener<I> itemClickListener;

    public void setOnItemClickListener(OnItemClickListener<I> listener) {
        if (allowSetClickListener()) {
            this.itemClickListener = listener;
        }
//        else {
//            throw new UnsupportedOperationException("setting click listener is not allowed");
//        }
    }

    public interface OnItemLongClickListener<I> {

        /**
         * @return true if event consumed
         */
        boolean onItemLongClick(int position, I item);
    }

    protected OnItemLongClickListener<I> itemLongClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener<I> listener) {
        if (allowSetLongClickListener()) {
            this.itemLongClickListener = listener;
        }
//        else {
//            throw new UnsupportedOperationException("setting long click listener is not allowed");
//        }
    }

    public interface OnProcessingItemListener<I, VH extends RecyclerView.ViewHolder> {
        void onProcessingItem(@NonNull VH holder, @Nullable I item, int position);
    }

    private OnProcessingItemListener<I, VH> processingItemListener;

    public void setOnProcessingItemListener(OnProcessingItemListener<I, VH> l) {
        this.processingItemListener = l;
    }

    @Override
    @CallSuper
    public void onViewRecycled(VH holder) {
        super.onViewRecycled(holder);
        holder.itemView.setOnClickListener(null);
    }

    public interface OnItemAddedListener<I> {
        void onItemAdded(int to, I item);
    }

    private OnItemAddedListener<I> itemAddedListener;

    public void setOnItemAddedListener(OnItemAddedListener<I> itemAddedListener) {
        this.itemAddedListener = itemAddedListener;
    }

    public interface OnItemSetListener<I> {
        void onItemSet(int to, I item);
    }

    private OnItemSetListener<I> itemSetListener;

    public void setOnItemSetListener(OnItemSetListener<I> itemSetListener) {
        this.itemSetListener = itemSetListener;
    }


    public interface OnItemRemovedListener<I> {
        void onItemRemoved(int from, I item);
    }

    private OnItemRemovedListener<I> itemRemovedListener;

    public void setOnItemRemovedListener(OnItemRemovedListener<I> itemRemovedListener) {
        this.itemRemovedListener = itemRemovedListener;
    }

    public static abstract class ViewHolder<I> extends RecyclerView.ViewHolder {

        @NonNull
        protected final Context context;

        public ViewHolder(@NonNull Context context, @NonNull View view) {
            super(view);
            this.context = context;
        }

        protected void displayData(int position, @NonNull final I item) {
            itemView.setVisibility(View.VISIBLE);
        }

        protected void displayNoData(int position, @Nullable final I item) {
            itemView.setVisibility(View.GONE);
        }
    }
}

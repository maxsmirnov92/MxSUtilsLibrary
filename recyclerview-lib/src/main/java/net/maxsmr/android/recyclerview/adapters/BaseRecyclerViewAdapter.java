package net.maxsmr.android.recyclerview.adapters;

import android.annotation.SuppressLint;
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
            if (notifyOnChange) {
                notifyDataSetChanged();
            }
        }
    }

    /**
     * @param items null for reset adapter
     */
    @SuppressLint("NewApi")
    public final void setItems(@Nullable Collection<I> items) {
        synchronized (mItems) {
            if (!Objects.equals(mItems, items)) {
                clearItems();
                if (items != null) {
                    this.mItems.addAll(items);
                }
                onItemsSet();
            }
        }
    }

    protected void onItemsSet() {
        if (notifyOnChange) {
            notifyDataSetChanged();
        }
        if (itemSetListener != null) {
            itemSetListener.onItemsSet(getItems());
        }
    }

    public final void clearItems() {
        synchronized (mItems) {
            if (!isEmpty()) {
                int previousSize = getItemCount();
                mItems.clear();
                onItemRangeRemoved(0, previousSize - 1, previousSize);
            }
        }
    }

    protected void onItemsCleared(int previousSize) {

    }

    public final void addItem(int to, @Nullable I item) throws IndexOutOfBoundsException {
        synchronized (mItems) {
            rangeCheckForAdd(to);
            mItems.add(to, item);
            onItemAdded(to, item);
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
    protected void onItemAdded(int to, @Nullable I item) {
        if (notifyOnChange) {
            if (to == 0) {
                notifyDataSetChanged();
            } else {
                notifyItemInserted(to);
            }
        }
        if (itemAddedListener != null) {
            itemAddedListener.onItemAdded(to, item);
        }
    }

    public final void setItem(int in, @Nullable I item) {
        synchronized (mItems) {
            rangeCheck(in);
            mItems.set(in, item);
            onItemSet(in, item);
        }
    }

    @CallSuper
    protected void onItemSet(int in, @Nullable I item) {
        if (notifyOnChange) {
            notifyItemChanged(in);
        }
        if (itemSetListener != null) {
            itemSetListener.onItemSet(in, item);
        }
    }

    // TODO replace item and replace range

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
        synchronized (mItems) {
            int previousSize = getItemCount();
            List<I> removed = new ArrayList<>();
            int position = 0;
            Iterator<I> iterator = mItems.iterator();
            while (iterator.hasNext()) {
                if (position >= from && position <= to) {
                    I item = iterator.next();
                    iterator.remove();
                    removed.add(item);
                }
                position++;
            }
            if (!removed.isEmpty()) {
                onItemRangeRemoved(from, to, previousSize);
            }
            return removed;
        }
    }

    public final void removeAllItems() {
        synchronized (mItems) {
            if (!isEmpty()) {
                removeItemsRange(0, getItemCount() - 1);
            }
        }
    }

    @CallSuper
    protected void onItemRangeRemoved(int from, int to, int previousSize) {
        if (notifyOnChange) {
            notifyItemRangeChanged(from, to);
        }
        if (itemRangeRemovedListener != null) {
            itemRangeRemovedListener.onItemRangeRemoved(from, to, previousSize);
        }
        if (from == 0 && to == previousSize - 1) {
            onItemsCleared(previousSize);
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
        holder.onViewRecycled();
        if (allowSetClickListener()) {
            holder.itemView.setOnClickListener(null);
        }
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
        void onItemsSet(@NonNull List<I> items);
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

    public interface OnItemRangeRemovedListener {
        void onItemRangeRemoved(int from, int to, int previousSize);
    }

    private OnItemRangeRemovedListener itemRangeRemovedListener;

    public void setOnItemRangeRemovedListener(OnItemRangeRemovedListener itemRangeRemovedListener) {
        this.itemRangeRemovedListener = itemRangeRemovedListener;
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

        protected void onViewRecycled() {

        }
    }
}

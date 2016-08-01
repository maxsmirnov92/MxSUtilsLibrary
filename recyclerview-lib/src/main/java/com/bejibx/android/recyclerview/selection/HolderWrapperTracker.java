package com.bejibx.android.recyclerview.selection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HolderWrapperTracker {

    private final SparseArray<SelectionHelper.ViewHolderWrapper> mWrappersByPosition = new SparseArray<>();

    public void bindWrapper(SelectionHelper.ViewHolderWrapper wrapper, int position) {
        mWrappersByPosition.put(position, wrapper);
    }

    public void recycleWrapper(int position) {
        SelectionHelper.ViewHolderWrapper wrapper = mWrappersByPosition.get(position);
        if (wrapper != null) {
            if (wrapper.getHolder() != null) {
                if (wrapper instanceof SelectionHelper.ViewHolderMultiSelectionWrapper) {
                    Set<SelectionHelper.SelectMode> selectModes = ((SelectionHelper.ViewHolderMultiSelectionWrapper) wrapper).getSelectModes();
                    if (selectModes.contains(SelectionHelper.SelectMode.CLICK)) {
                        wrapper.getHolder().itemView.setOnClickListener(null);
                    }
                    if (selectModes.contains(SelectionHelper.SelectMode.LONG_CLICK)) {
                        wrapper.getHolder().itemView.setOnLongClickListener(null);
                    }
                } else if (wrapper instanceof SelectionHelper.ViewHolderClickWrapper) {
                    wrapper.getHolder().itemView.setOnClickListener(null);
                }
            }
            mWrappersByPosition.remove(position);
        }
    }

    @Nullable
    public SelectionHelper.ViewHolderWrapper getWrapper(int position) {
        SelectionHelper.ViewHolderWrapper wrapper = mWrappersByPosition.get(position);

        boolean correct = true;

        if (wrapper == null) {
            correct = false;
        } else {
            if (wrapper.getHolder() != null) {
                int adapterPosition = wrapper.getHolder().getAdapterPosition();
                if (adapterPosition != position && wrapper.getHolder().getAdapterPosition() != RecyclerView.NO_POSITION) {
                    correct = false;
                }
            } else {
                correct = false;
            }
        }


        if (!correct) {
            mWrappersByPosition.remove(position);
            return null;
        }

        return wrapper;
    }

    @NonNull
    public List<SelectionHelper.ViewHolderWrapper> getTrackedWrappers() {
        List<SelectionHelper.ViewHolderWrapper> wrappers = new ArrayList<>();

        for (int i = 0; i < mWrappersByPosition.size(); i++) {
            int key = mWrappersByPosition.keyAt(i);
            SelectionHelper.ViewHolderWrapper wrapper = getWrapper(key);
            if (wrapper != null) {
                wrappers.add(wrapper);
            }
        }

        return wrappers;
    }

    public void clear() {
        mWrappersByPosition.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        clear();
    }
}

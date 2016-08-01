package com.bejibx.android.recyclerview.selection;

import android.support.v7.widget.RecyclerView;

public interface SelectionObserver {
    void onSelectedChanged(RecyclerView.ViewHolder holder, boolean isSelected, boolean fromUser);

    void onSelectableChanged(boolean isSelectable);
}

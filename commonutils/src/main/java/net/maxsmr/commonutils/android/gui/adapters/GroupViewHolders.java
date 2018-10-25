package net.maxsmr.commonutils.android.gui.adapters;


import android.content.Context;
import android.support.annotation.LayoutRes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class GroupViewHolders {

    public GroupViewHolders() {
        throw new AssertionError("no instances.");
    }

    public static void expandAllGroups(ExpandableListView listView) {
        if (listView != null && listView.getExpandableListAdapter() != null) {
            for (int i = 0; i < listView.getExpandableListAdapter().getGroupCount(); i++) {
                listView.expandGroup(i);
            }
        }
    }

    public static void collapseAllGroups(ExpandableListView listView) {
        if (listView != null && listView.getExpandableListAdapter() != null) {
            for (int i = 0; i < listView.getExpandableListAdapter().getGroupCount(); i++) {
                listView.collapseGroup(i);
            }
        }
    }

    public static abstract class BaseGroupViewHolder<I> {

        @LayoutRes
        protected final int itemLayoutId;

        protected View itemView;

        @NotNull
        protected ViewGroup parent;

        public BaseGroupViewHolder(int itemLayoutId, @Nullable View itemView, @NotNull ViewGroup parent) {
            this.itemLayoutId = itemLayoutId;
            this.itemView = itemView;
            this.parent = parent;
        }

        protected View createItemView() {
            if (itemView == null) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(itemLayoutId, parent, false);
            }
            return itemView;
        }

        public View getItemView() {
            return itemView;
        }

        protected abstract void onBindView(@NotNull View itemView);

        protected boolean isItemEmpty(@Nullable I item) {
            return item == null;
        }

        public final void fill(@Nullable I item, int groupPosition) {
            createItemView();
            onBindView(itemView);
            if (item == null || isItemEmpty(item)) {
                displayNoData(groupPosition);
            } else {
                displayData(item, groupPosition);
            }
        }

        public void displayData(@NotNull I item, int groupPosition) {
            itemView.setVisibility(View.VISIBLE);
        }

        public void displayNoData(int groupPosition) {
            itemView.setVisibility(View.GONE);
        }
    }

    public abstract static class BaseChildViewHolder<I> {

        @NotNull
        protected final Context context;

        @LayoutRes
        protected final int itemLayoutId;

        protected View itemView;

        @NotNull
        protected ViewGroup parent;

        public BaseChildViewHolder(@NotNull Context context, int itemLayoutId, @Nullable View itemView, @NotNull ViewGroup parent) {
            this.context = context;
            this.itemLayoutId = itemLayoutId;
            this.itemView = itemView;
            this.parent = parent;
        }

        protected View createItemView() {
            if (itemView == null) {
                itemView = LayoutInflater.from(context).inflate(itemLayoutId, parent, false);
            }
            return itemView;
        }

        public View getItemView() {
            return itemView;
        }

        protected abstract void onBindView(@NotNull View itemView);

        public final void fill(@Nullable I item, int groupPosition, int childPosition) {
            createItemView();
            onBindView(itemView);
            if (item == null) {
                displayNoData(groupPosition, childPosition);
            } else {
                displayData(item, groupPosition, childPosition);
            }
        }

        public void displayData(@NotNull I item, int groupPosition, int childPosition) {
            itemView.setVisibility(View.VISIBLE);
        }

        public void displayNoData(int groupPosition, int childPosition) {
            itemView.setVisibility(View.GONE);
        }
    }
}

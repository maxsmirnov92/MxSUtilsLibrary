package net.maxsmr.utilstest;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import net.maxsmr.android.recyclerview.adapters.BaseRecyclerViewAdapter;
import net.maxsmr.jugglerhelper.fragments.base.BaseListJugglerFragment;

import java.util.Comparator;
import java.util.List;

import me.ilich.juggler.gui.JugglerFragment;
import me.ilich.juggler.states.ContentOnlyState;
import me.ilich.juggler.states.VoidParams;

public class TState extends ContentOnlyState<VoidParams> {

    public TState(@Nullable VoidParams params) {
        super(params);
    }

    @Override
    protected JugglerFragment onConvertContent(VoidParams params, @Nullable JugglerFragment fragment) {
        return new BaseListJugglerFragment() {
            @Override
            protected int getContentLayoutId() {
                return R.layout.recycler;
            }

            @Override
            protected int getBaseItemLayoutId() {
                return 0;
            }

            @NonNull
            @Override
            protected BaseRecyclerViewAdapter initAdapter() {
                return new BaseRecyclerViewAdapter(getContext(), 0, null) {
                    @Override
                    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        return null;
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

                    }
                };
            }

            @Override
            protected boolean allowSetInitialItems() {
                return false;
            }

            @Nullable
            @Override
            protected List getInitialItems() {
                return null;
            }

            @Nullable
            @Override
            protected Comparator getSortComparator() {
                return null;
            }

            @Override
            protected boolean isDuplicateItems(@Nullable Object one, @Nullable Object another) {
                return false;
            }
        };
    }
}

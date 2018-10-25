package net.maxsmr.commonutils.android.gui.views.recycler;

import org.jetbrains.annotations.NotNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class RecyclerScrollableController2 extends RecyclerView.OnScrollListener {

    @NotNull
    protected final RecyclerView recyclerView;

    protected RecyclerScrollableController.OnLastItemVisibleListener listener;

    private int previousTotal = 0;

    private boolean loading = true;

    private boolean forceLoading = false;

    public RecyclerScrollableController2(RecyclerScrollableController.OnLastItemVisibleListener listener, @NotNull RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        setListener(listener);
    }

    public int getCurrentTotalItemCount() {
        return recyclerView.getLayoutManager().getItemCount();
    }

    public int getPreviousTotalItemCount() {
        return previousTotal;
    }

    public synchronized void setListener(RecyclerScrollableController.OnLastItemVisibleListener listener) {
        this.listener = listener;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isForceLoading() {
        return forceLoading;
    }

    public synchronized void setForceLoading(boolean forceLoading) {
        if (forceLoading != this.forceLoading) {
            this.forceLoading = forceLoading;
            if (forceLoading) {
                loading = true;
            } else {
                doCheck(recyclerView);
            }
        }
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        doCheck(recyclerView);
    }

    private synchronized void doCheck(@NotNull RecyclerView recyclerView) {

        if (this.recyclerView != recyclerView) {
            throw new RuntimeException(RecyclerView.class.getSimpleName() + " not match");
        }

        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int totalItemCount = getCurrentTotalItemCount();

        boolean isLastItemVisible = layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.getItemCount() - 1;

        if ((loading || previousTotal == 0) && !forceLoading) {
            if (totalItemCount != previousTotal) {
                loading = false;
                previousTotal = totalItemCount;
            }
        }
        if (!loading && isLastItemVisible) {
            if (listener != null) {
                listener.onLastItemVisible();
            }
            loading = true;
        }
    }
}
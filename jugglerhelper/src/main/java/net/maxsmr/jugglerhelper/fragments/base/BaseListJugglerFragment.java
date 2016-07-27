package net.maxsmr.jugglerhelper.fragments.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.maxsmr.android.recyclerview.adapters.BaseRecyclerViewAdapter;
import net.maxsmr.commonutils.android.gui.GuiUtils;
import net.maxsmr.commonutils.android.gui.fonts.FontsHolder;
import net.maxsmr.commonutils.android.gui.progressable.DialogProgressable;
import net.maxsmr.commonutils.android.gui.progressable.Progressable;
import net.maxsmr.commonutils.android.gui.views.RecyclerScrollableController;
import net.maxsmr.jugglerhelper.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public abstract class BaseListJugglerFragment<I, Adapter extends BaseRecyclerViewAdapter<I, ? extends BaseRecyclerViewAdapter.ViewHolder<I>>> extends BaseJugglerFragment implements BaseRecyclerViewAdapter.OnItemClickListener<I>, BaseRecyclerViewAdapter.OnItemLongClickListener<I>, BaseRecyclerViewAdapter.OnItemAddedListener<I>, BaseRecyclerViewAdapter.OnItemSetListener<I>, BaseRecyclerViewAdapter.OnItemRemovedListener<I>, RecyclerScrollableController.OnLastItemVisibleListener, SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger(BaseListJugglerFragment.class);

    //    @BindView(R.id.swipeRefreshLayout)
    @Nullable
    SwipeRefreshLayout swipeRefreshLayout;

    //    @BindView(R.id.recycler)
    RecyclerView recycler;

    //    @BindView(R.id.emptyText)
    @Nullable
    TextView placeholder;

    //    @BindView(R.id.loading)
    @Nullable
    LinearLayout loadingLayout;

    //    @BindView(R.id.btRetry)
    @Nullable
    Button retryButton;

    private RecyclerScrollableController recyclerScrollableController;

    private Adapter adapter;

    private Progressable progressable;

    protected final Progressable getProgressable() {
        return progressable;
    }

    @LayoutRes
    protected abstract int getItemLayoutId();

    @Nullable
    protected Progressable initProgressable() {
        return newLoadListProgressable();
    }

    @CallSuper
    protected void onBindViews(@NonNull View rootView) {
//        ButterKnife.bind(this, rootView);
        swipeRefreshLayout = findViewById(rootView, R.id.swipeRefreshLayout);
        recycler = findViewById(rootView, R.id.recycler);
        placeholder = findViewById(rootView, R.id.emptyText);
        loadingLayout = findViewById(rootView, R.id.loading);
        retryButton = findViewById(rootView, R.id.btRetry);
    }


    @CallSuper
    protected void init() {

        View rootView = getView();

        if (rootView == null) {
            throw new IllegalStateException("root view was not created");
        }

        if (recycler == null) {
            throw new RuntimeException("recycler view not found");
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this);
            swipeRefreshLayout.setEnabled(enableSwipeRefresh());
        }

        adapter = initAdapter();
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        adapter.setOnItemAddedListener(this);
        adapter.setOnItemSetListener(this);
        adapter.setOnItemRemovedListener(this);

        recycler.setLayoutManager(getRecyclerLayoutManager());
        RecyclerView.ItemDecoration itemDecoration = getItemDecoration();
        if (itemDecoration != null) {
            recycler.addItemDecoration(itemDecoration);
        }
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(recyclerScrollableController = new RecyclerScrollableController(this));

        setLayoutParams();

        progressable = initProgressable();

        if (retryButton != null) {
            retryButton.setOnClickListener(this);
        }

        applyTypeface();
    }

    protected void postInit() {

    }

    @CallSuper
    protected void applyTypeface() {

        View rootView = getView();

        if (rootView == null) {
            throw new IllegalStateException("root view was not created");
        }

        String alias = getFontAlias();

        if (!TextUtils.isEmpty(alias)) {
            FontsHolder.getInstance().apply(placeholder, alias, false);
            FontsHolder.getInstance().apply(loadingLayout, alias, false);
            FontsHolder.getInstance().apply(retryButton, alias, false);
        }
    }

    private void setLayoutParams() {

        View rootView = getView();

        if (rootView == null) {
            throw new RuntimeException("root view is not inflated");
        }

        Pair<Integer, Integer> size = getRecyclerSize();

        if (size != null) {

            int width = size.first;
            int height = size.second;

            if ((width != ViewGroup.LayoutParams.MATCH_PARENT && width != ViewGroup.LayoutParams.WRAP_CONTENT && width <= 0) || (height != ViewGroup.LayoutParams.MATCH_PARENT && height != ViewGroup.LayoutParams.WRAP_CONTENT && height <= 0)) {
                throw new RuntimeException("incorrect width or height");
            }

            ViewGroup.LayoutParams rootParams = rootView.getLayoutParams();
            rootParams.width = width;
            rootParams.height = height;
            rootView.setLayoutParams(rootParams);

            ViewGroup.LayoutParams recyclerParams = recycler.getLayoutParams();
            recyclerParams.width = width;
            recyclerParams.height = height;
            recycler.setLayoutParams(recyclerParams);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        if (allowSetInitialItems()) {
            reloadAdapter(getInitialItems());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        postInit();
    }

    protected boolean enableSwipeRefresh() {
        return true;
    }

    /**
     * @return pair with width and height {@link ViewGroup.LayoutParams#WRAP_CONTENT or @link android.view.ViewGroup.LayoutParams#MATCH_PARENT}; null if changing layout params not allowed
     */
    @Nullable
    protected Pair<Integer, Integer> getRecyclerSize() {
        return null;
    }

    @NonNull
    protected RecyclerView.LayoutManager getRecyclerLayoutManager() {
        return new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    }

    @Nullable
    protected RecyclerView.ItemDecoration getItemDecoration() {
        return null;
    }

    protected final RecyclerView getRecyclerView() {
        return recycler;
    }

    protected final Adapter getAdapter() {
        return adapter;
    }

    @NonNull
    protected abstract Adapter initAdapter();

    protected abstract boolean allowSetInitialItems();

    @Nullable
    protected abstract List<I> getInitialItems();

    protected synchronized void reloadAdapter(@Nullable List<I> items) {
        logger.debug("reloadAdapter(), items=" + items + ", this=" + this);
        if (adapter != null) {
            sortAndRemoveDuplicateItemsFromList(items);
            adapter.setItems(items);
            if (items != null && !items.isEmpty()) {
                onLoaded(items);
            } else {
                onEmpty();
            }
        }
    }

    protected final void onStartLoading() {
        if (progressable != null) {
            progressable.onStart();
            if (progressable instanceof DialogProgressable) {
                GuiUtils.setProgressBarColor(ContextCompat.getColor(getContext(), R.color.progress_primary), ((DialogProgressable) progressable).getProgressBar());
                String alias = getFontAlias();
                if (TextUtils.isEmpty(alias)) {
                    FontsHolder.getInstance().apply(((DialogProgressable) progressable).getMessageView(), alias, false);
                }
            }
        }
    }

    protected final void onStopLoading() {
        if (progressable != null) {
            progressable.onStop();
        }
    }

    @Override
    public final void onRefresh() {
        if (progressable instanceof BaseListJugglerFragment.LoadListProgressable) {
            doRefreshList();
        } else {
            throw new RuntimeException("progressable is not instance of " + LoadListProgressable.class);
        }
    }

    protected abstract void doRefreshList();

    protected void loading(boolean toggle) {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(toggle ? View.VISIBLE : View.GONE);
        }
        recycler.setVisibility(View.GONE);
        if (placeholder != null) {
            placeholder.setVisibility(View.GONE);
        }
        if (retryButton != null) {
            retryButton.setVisibility(View.GONE);
        }
        if (swipeRefreshLayout != null && swipeRefreshLayout.isEnabled()) {
            if (swipeRefreshLayout.isRefreshing() != toggle) {
                swipeRefreshLayout.setRefreshing(toggle);
            }
        }
        if (!toggle) {
            processEmpty();
        }
    }

    protected void processEmpty() {
        if (adapter != null) {
            boolean hasItems = adapter.getItemCount() > 0;
            recycler.setVisibility(hasItems ? View.VISIBLE : View.GONE);
            if (placeholder != null) {
                placeholder.setVisibility(hasItems ? View.GONE : View.VISIBLE);
                if (!hasItems) {
                    placeholder.setText(getEmptyText());
                }
            }
            if (retryButton != null) {
                retryButton.setVisibility(View.GONE);
            }
        }
    }

    @CallSuper
    protected void processError() {
        if (adapter != null) {
            recycler.setVisibility(View.GONE);
            if (placeholder != null) {
                placeholder.setVisibility(View.VISIBLE);
                placeholder.setText(getErrorText());
            }
            if (retryButton != null) {
                retryButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @CallSuper
    protected void unlisten() {

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(null);
        }

        adapter.setOnItemClickListener(null);
        adapter.setOnItemLongClickListener(null);
        adapter.setOnItemAddedListener(null);
        adapter.setOnItemSetListener(null);
        adapter.setOnItemRemovedListener(null);

        recycler.removeOnScrollListener(recyclerScrollableController);
        recyclerScrollableController = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unlisten();
    }

    @Nullable
    protected abstract Comparator<I> getSortComparator();

    protected abstract boolean isDuplicateItems(@Nullable I one, @Nullable I another);

    /**
     * @param items will be modified if contains duplicate items
     * @return removed duplicate items
     */
    @NonNull
    private List<I> sortAndRemoveDuplicateItemsFromList(@Nullable List<I> items) {

        final Comparator<I> comparator = getSortComparator();

        List<I> duplicateItems = new ArrayList<>();

        if (items != null) {

            if (comparator != null) {
                Collections.sort(items, comparator);
            }

            Iterator<I> iterator = items.iterator();

            int index = -1;

            while (iterator.hasNext()) {

                I item = iterator.next();
                index++;

                boolean isDuplicate = false;

                for (int i = index + 1; i < items.size(); i++) {
                    I compareItem = items.get(i);
                    if (isDuplicateItems(compareItem, item)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate || item == null) {
                    logger.debug("removing duplicate item: " + item + "...");
                    iterator.remove();
                    index--;
                    duplicateItems.add(item);
                }
            }

            if (comparator != null) {
                Collections.sort(items, comparator);
                logger.debug("sorted items: " + items);
            }
        }

        return duplicateItems;
    }

    @Nullable
    protected String getEmptyText() {
        return getContext().getString(R.string.data_missing);
    }

    @Nullable
    protected String getErrorText() {
        return getContext().getString(R.string.data_load_failed);
    }

    @Override
    public void onItemClick(int position, I item) {
        logger.debug("onItemClick(), position=" + position + ", item=" + item);
    }

    @Override
    public boolean onItemLongClick(int position, I item) {
        logger.debug("onItemLongClick()), position=" + position + ", item=" + item);
        return false;
    }

    @Override
    public void onItemAdded(int to, I item) {
        logger.debug("onItemAdded(), to=" + to + ", item=" + item);
    }

    @Override
    public void onItemSet(int to, I item) {
        logger.debug("onItemSet(), to=" + to + ", item=" + item);
    }

    @Override
    public void onItemRemoved(int from, I item) {
        logger.debug("onItemRemoved(), from=" + from + ",  item=" + item);
    }

    @Override
    public void onLastItemVisible() {

    }

    @CallSuper
    protected void onLoaded(List<I> items) {
        processEmpty();
    }

    @CallSuper
    protected void onEmpty() {
        processEmpty();
    }


    //    @OnClick(R.id.btRetry)
//    @Optional
    protected void onRetryClick() {
        if (allowSetInitialItems()) {
            reloadAdapter(getInitialItems());
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btRetry) {
            onRetryClick();
        }
    }

    protected class LoadListProgressable implements Progressable {

        public LoadListProgressable() {
            if (getContext() == null) {
                throw new IllegalStateException("fragment is not attached");
            }
            if (loadingLayout != null) {
                GuiUtils.setProgressBarColor(ContextCompat.getColor(getContext(), R.color.progress_primary), (ProgressBar) loadingLayout.findViewById(R.id.pbLoading));
            }
            if (swipeRefreshLayout != null) {
//                swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.progressBarColor);
                swipeRefreshLayout.setColorSchemeResources(
                        R.color.progress_start,
                        R.color.progress_primary,
                        R.color.progress_end);
            }
        }

        @Override
        public void onStart() {
            if (isAdded()) {
                loading(true);
            }
        }

        @Override
        public void onStop() {
            if (isAdded()) {
                loading(false);
            }
        }
    }

    protected final Progressable newLoadListProgressable() {
        return new LoadListProgressable();
    }

    protected final Progressable newDialogProgressable() {
        return new DialogProgressable(getContext());
    }
}

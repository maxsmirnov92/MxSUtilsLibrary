package net.maxsmr.jugglerhelper.fragments.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;

import net.maxsmr.commonutils.android.gui.adapters.CustomFragmentStatePagerAdapter;
import net.maxsmr.commonutils.android.gui.fonts.FontsHolder;
import net.maxsmr.jugglerhelper.R;

import java.lang.reflect.Field;

public abstract class BaseTabsJugglerFragment<PagerAdapter extends CustomFragmentStatePagerAdapter> extends BaseJugglerFragment {

    @NonNull
    protected abstract PagerAdapter initStatePagerAdapter();

    @SuppressWarnings("unchecked")
    @Nullable
    protected final PagerAdapter getStatePagerAdapter() {
        return (PagerAdapter) viewPager.getAdapter();
    }

//    @BindView(R.id.tab_layout)
    protected TabLayout tabLayout;

//    @BindView(R.id.pager)
    protected ViewPager viewPager;

    @Override
    @CallSuper
    protected void onBindViews(@NonNull View rootView) {
        tabLayout = findViewById(rootView, R.id.tab_layout);
        viewPager = findViewById(rootView, R.id.pager);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @TabLayout.TabGravity
    protected int getTabGravity() {
        return TabLayout.GRAVITY_CENTER;
    }

    @TabLayout.Mode
    protected int getTabMode() {
        return TabLayout.MODE_FIXED;
    }

    @SuppressWarnings("WrongConstant")
    @CallSuper
    protected void init() {

        if (viewPager == null) {
            throw new RuntimeException("viewPager not found");
        }

        if (tabLayout == null) {
            throw new RuntimeException("tabLayout not found");
        }

        int tabGravity = getTabGravity();
        if (tabGravity != TabLayout.GRAVITY_CENTER && tabGravity != TabLayout.GRAVITY_FILL) {
            throw new IllegalArgumentException("incorrect tabGravity: " + tabGravity);
        }
        tabLayout.setTabGravity(tabGravity);

        int tabMode = getTabMode();
        if (tabMode != TabLayout.MODE_FIXED && tabMode != TabLayout.MODE_SCROLLABLE) {
            throw new IllegalArgumentException("incorrect tabMode: " + tabMode);
        }
        tabLayout.setTabMode(tabMode);

        reload();
    }

    protected void setTabsTypeface(String alias) {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                Field field = null;
                try {
                    field = tab.getClass().getDeclaredField("mView");
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                if (field != null) {
                    View tabView = null;
                    try {
                        field.setAccessible(true);
                        tabView = (View) field.get(tab);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    FontsHolder.getInstance().apply(tabView, alias, false);
                }
            }
        }
    }

    @CallSuper
    protected void reload() {

        if (getContext() == null) {
            throw new RuntimeException("not attached to activity");
        }

        String alias = getFontAlias();

        if (!TextUtils.isEmpty(alias)) {
            setTabsTypeface(alias);
        }

        viewPager.setAdapter(initStatePagerAdapter());
        tabLayout.setupWithViewPager(viewPager);

        notifyFragmentsChanged();
    }

    private void updateTabIcons() {
        PagerAdapter adapter = getStatePagerAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                Fragment f = adapter.getFragmentInstance(i);
                Drawable tabIcon = getTabIconForFragment(f);
                if (tabIcon != null) {
                    TabLayout.Tab tab = tabLayout.getTabAt(i);
                    if (tab != null) {
                        tab.setIcon(tabIcon);
                    }
                }
            }
        }
    }

    @Nullable
    protected abstract Drawable getTabIconForFragment(Fragment f);

//    @Nullable
//    protected abstract Pair<Integer, Integer> getTabTextColors();

    protected void notifyFragmentsChanged() {

        if (getContext() == null) {
            throw new RuntimeException("not attached to activity");
        }

        PagerAdapter adapter = getStatePagerAdapter();

        if (adapter == null) {
            throw new RuntimeException("adapter is not initialized");
        }

        adapter.setNeedToNotify(true);
        adapter.notifyDataSetChanged();

        updateTabIcons();
    }

    public void selectTab(int at) {

        if (at < 0 || at >= tabLayout.getTabCount()) {
            throw new IndexOutOfBoundsException("incorrect tab index: " + at);
        }

        TabLayout.Tab tab = tabLayout.getTabAt(at);
        if (tab != null && !tab.isSelected()) {
            tab.select();
        }
    }

    public void selectTabByFragment(Fragment fragment) {
        PagerAdapter adapter = getStatePagerAdapter();

        if (adapter == null) {
            throw new RuntimeException("adapter is not initialized");
        }

        int index = adapter.fragmentIndexOf(fragment);
        if (index >= 0) {
            selectTab(index);
        }
    }



}

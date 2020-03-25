package net.maxsmr.commonutils.android.gui.fragments.adapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import net.maxsmr.commonutils.data.Pair;
import net.maxsmr.commonutils.data.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.maxsmr.commonutils.data.CompareUtilsKt.objectsEqual;
import static net.maxsmr.commonutils.data.CompareUtilsKt.stringsEqual;

public class CustomFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

    @NotNull
    protected final Context context;

    @NotNull
    private final List<FragmentPair> fragments = new ArrayList<>();

    @NotNull
    private final Map<Fragment, Boolean> fragmentStateMap = new HashMap<>();

    private FragmentStateListener listener;

    private boolean needToNotify = true;

    public CustomFragmentStatePagerAdapter(@NotNull Context context, @NotNull FragmentManager fm) {
        super(fm);
        this.context = context;
    }

    public boolean isNeedToNotify() {
        return needToNotify;
    }

    public void setNeedToNotify(boolean needToNotify) {
        this.needToNotify = needToNotify;
    }

    public void setFragmentStateListener(FragmentStateListener listener) {
        this.listener = listener;
    }

    @NotNull
    public List<FragmentPair> getFragmentPairs() {
        return new ArrayList<>(fragments);
    }

    @NotNull
    public List<Fragment> getFragments() {
        List<Fragment> fragments = new ArrayList<>();
        for (FragmentPair p : this.fragments) {
            if (p != null) {
                fragments.add(p.fragment);
            }
        }
        return fragments;
    }

    @Nullable
    public Map<Integer, FragmentPair> filterByInstance(@Nullable FragmentPair targetFragment) {
        return filterByInstance(targetFragment != null? targetFragment.fragment : null);
    }

    @Nullable
    public Map<Integer, FragmentPair> filterByInstance(@Nullable Fragment targetFragment) {
        return Predicate.Methods.filterWithIndex(fragments, getFilterByInstancePredicate(targetFragment));
    }

    @Nullable
    public Map<Integer, FragmentPair> filterByTitle(@Nullable FragmentPair targetFragment, boolean ignoreCase) {
        return filterByTitle(targetFragment != null? targetFragment.title : null, ignoreCase);
    }

    @Nullable
    public Map<Integer, FragmentPair> filterByTitle(@Nullable String title, boolean ignoreCase) {
        return Predicate.Methods.filterWithIndex(fragments, getFilterByTitlePredicate(title, ignoreCase));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public Map<Integer, FragmentPair> filterByClass(@Nullable FragmentPair targetFragment) {
        return filterByClass(targetFragment != null? (Class<Fragment>) targetFragment.fragment.getClass() : null);
    }

    @Nullable
    public Map<Integer, FragmentPair> filterByClass(@Nullable Class<? extends Fragment> fragmentClass) {
        return Predicate.Methods.filterWithIndex(fragments, getFilterByClassPredicate(fragmentClass));
    }

    @Nullable
    public Pair<Integer, FragmentPair> findByInstance(@Nullable FragmentPair targetFragment) {
        return findByInstance(targetFragment != null? targetFragment.fragment : null);
    }

    @Nullable
    public Pair<Integer, FragmentPair> findByInstance(@Nullable Fragment targetFragment) {
        return Predicate.Methods.findWithIndex(fragments, getFilterByInstancePredicate(targetFragment));
    }

    @Nullable
    public Pair<Integer, FragmentPair> findByTitle(@Nullable FragmentPair targetFragment, boolean ignoreCase) {
        return findByTitle(targetFragment != null? targetFragment.title : null, ignoreCase);
    }

    @Nullable
    public Pair<Integer, FragmentPair> findByTitle(@Nullable String title, boolean ignoreCase) {
        return Predicate.Methods.findWithIndex(fragments, getFilterByTitlePredicate(title, ignoreCase));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public Pair<Integer, FragmentPair> findByClass(@Nullable FragmentPair targetFragment) {
        return findByClass(targetFragment != null? (Class<Fragment>) targetFragment.fragment.getClass() : null);
    }

    @Nullable
    public Pair<Integer, FragmentPair> findByClass(@Nullable Class<? extends Fragment> fragmentClass) {
        return Predicate.Methods.findWithIndex(fragments, getFilterByClassPredicate(fragmentClass));
    }

    @Nullable
    public FragmentPair getFragmentPair(int position) {
        rangeCheck(position);
        return fragments.get(position);
    }

    @Nullable
    public Fragment getFragmentInstance(int position) {
        final FragmentPair pair = getFragmentPair(position);
        if (pair != null) {
            return pair.fragment;
        }
        return null;
    }

    @Nullable
    public String getFragmentTitle(int position) {
        final FragmentPair pair = getFragmentPair(position);
        if (pair != null) {
            return pair.title;
        }
        return null;
    }

    public void addFragments(@Nullable List<FragmentPair> pairs) {
        if (pairs != null) {
            fragments.addAll(pairs);
            notifyDataSetChanged();
        }
    }


    public void addFragment(@NotNull Fragment fragment, String title) {
        addFragment(new FragmentPair(fragment, title));
    }

    public void addFragment(@NotNull Fragment fragment, @StringRes int titleRes) {
        addFragment(fragment, context.getString(titleRes));
    }

    public void addFragment(@Nullable FragmentPair p) {
        if (p != null) {
            fragments.add(p);
            notifyDataSetChanged();
        }
    }

    public void setFragments(@Nullable Collection<FragmentPair> fragments) {
        this.fragments.clear();
        if (fragments != null) {
            this.fragments.addAll(fragments);
        }
        notifyDataSetChanged();
    }

    @Nullable
    public Fragment removeFragment(int position) {
        rangeCheck(position);
        FragmentPair pair = fragments.get(position);
        if (pair != null) {
            Fragment removed = pair.fragment;
            fragments.remove(position);
            notifyDataSetChanged();
            return removed;
        }
        return null;
    }

    public void clear() {
        fragments.clear();
        notifyDataSetChanged();
    }

    public final boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public final int getCount() {
        return fragments.size();
    }

    @Override
    public final Fragment getItem(int position) {
        return getFragmentInstance(position);
    }

    @Override
    public final CharSequence getPageTitle(int position) {
        return getFragmentTitle(position);
    }

    @Override
    public int getItemPosition(@NotNull Object object) {
        return POSITION_NONE;
    }

    @NotNull
    @Override
    public Object instantiateItem(@NotNull ViewGroup container, int position) {
        final FragmentPair pair = fragments.get(position);
        if (pair == null) {
            throw new IllegalStateException(FragmentPair.class.getSimpleName() + " cannot be null");
        }
        fragmentStateMap.put(pair.fragment, true);
        return super.instantiateItem(container, position);
    }

    @Override
    public void destroyItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
        final FragmentPair pair = fragments.get(position);
        if (pair == null) {
            throw new IllegalStateException(FragmentPair.class.getSimpleName() + " cannot be null");
        }
        fragmentStateMap.put(pair.fragment, false);
        super.destroyItem(container, position, object);
    }

    @Override
    public void notifyDataSetChanged() {
        if (needToNotify)
            super.notifyDataSetChanged();
    }

    @Override
    public void finishUpdate(@NotNull ViewGroup container) {
        super.finishUpdate(container);
        if (listener != null) {
            for (FragmentPair p : fragments) {
                Boolean attached = fragmentStateMap.get(p.fragment);
                if (attached != null) {
                    if (attached) {
                        listener.onFragmentAttach(p.fragment);
                    } else {
                        listener.onFragmentDetach(p.fragment);
                    }
                }
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        fragments.clear();
        fragmentStateMap.clear();
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= fragments.size()) {
            throw new IndexOutOfBoundsException("incorrect fragment index: " + index);
        }
    }

    private Predicate<FragmentPair> getFilterByInstancePredicate(@Nullable Fragment fragment) {
        return item -> item != null && objectsEqual(item, fragment);
    }

    private Predicate<FragmentPair> getFilterByTitlePredicate(@Nullable String title, boolean ignoreCase) {
        return item -> item != null && stringsEqual(item.title, title, ignoreCase);
    }

    private Predicate<FragmentPair> getFilterByClassPredicate(@Nullable Class<? extends Fragment> fragmentClass) {
        return item -> item == null || fragmentClass != null && fragmentClass.isAssignableFrom(item.fragment.getClass());
    }

    public interface FragmentStateListener {

        void onFragmentAttach(Fragment f);

        void onFragmentDetach(Fragment f);
    }

    public static class FragmentPair {

        @NotNull
        public final Fragment fragment;

        @Nullable
        public final String title;

        protected FragmentPair(@NotNull Fragment fragment, @Nullable String title) {
            this.fragment = fragment;
            this.title = title;
        }

        @NotNull
        @Override
        public String toString() {
            return "FragmentPair{" +
                    "fragment=" + fragment +
                    ", title='" + title + '\'' +
                    '}';
        }
    }
}

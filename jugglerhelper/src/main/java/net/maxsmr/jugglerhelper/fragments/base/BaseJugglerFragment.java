package net.maxsmr.jugglerhelper.fragments.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import net.maxsmr.commonutils.android.gui.GuiUtils;
import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.jugglerhelper.R;

import java.util.List;

import me.ilich.juggler.gui.JugglerActivity;
import me.ilich.juggler.gui.JugglerFragment;
import me.ilich.nestableviewpager.NestablePagerItem;

public abstract class BaseJugglerFragment extends JugglerFragment implements NestablePagerItem {

    @Nullable
    @SuppressWarnings("unchecked")
    public <F extends JugglerFragment> F findFragment(Class<F> fragmentClass) {
        FragmentManager fm = getChildFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && !fragment.isDetached() && fragmentClass.isAssignableFrom(fragment.getClass())) {
                    return (F) fragment;
                }
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <F extends JugglerFragment> F findFragment(String tag) {
        FragmentManager fm = getChildFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && !fragment.isDetached() && CompareUtils.stringsEqual(fragment.getTag(), tag, false)) {
                    return (F) fragment;
                }
            }
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <F extends JugglerFragment> F findFragment(int id) {
        FragmentManager fm = getChildFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && !fragment.isDetached() && CompareUtils.objectsEqual(fragment.getId(), id)) {
                    return (F) fragment;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    protected <T extends JugglerActivity> T getBaseActivity() {
        FragmentActivity activity = getActivity();

        if (activity == null) {
            throw new NullPointerException("not attached to activity");
        }
        if (!(activity instanceof JugglerActivity)) {
            throw new RuntimeException("activity isn't instance of a BaseActivity");
        }
        return (T) activity;
    }

    @ColorInt
    protected int getStatusBarColor() {
        return ContextCompat.getColor(getContext(), R.color.colorPrimaryDark);
    }

    @ColorInt
    protected int getNavigationBarColor() {
        return ContextCompat.getColor(getContext(), R.color.navigationBarColor);
    }

    @Nullable
    protected String getBaseFontAlias() {
        return null;
    }

    @Nullable
    private Bundle savedInstanceState;

    @Nullable
    public Bundle getSavedInstanceState() {
        return savedInstanceState;
    }


    @LayoutRes
    protected abstract int getContentLayoutId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setHasOptionsMenu(false);
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(getContentLayoutId(), container, false);
        onBindViews(rootView);
        return rootView;
    }

    protected abstract void onBindViews(@NonNull View rootView);
//        ButterKnife.bind(this, rootView);

    private Menu menu;

    public Menu getMenu() {
        return this.menu;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
    }

    protected void init() {}

    protected void postInit() {}

    protected void unlisten() {}

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        postInit();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        GuiUtils.setStatusBarColor(getStatusBarColor(), getBaseActivity().getWindow());
        GuiUtils.setNavigationBarColor(getNavigationBarColor(), getBaseActivity().getWindow());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unlisten();
    }

    protected boolean clearFocus(View view) {
        if (getActivity() == null) {
            throw new IllegalStateException("fragment " + this + " is not attached to activity");
        }
        return !GuiUtils.clearFocus(view, getActivity());
    }

    protected boolean requestFocus(View view) {
        if (getActivity() == null) {
            throw new IllegalStateException("fragment " + this + " is not attached to activity");
        }
        return !GuiUtils.requestFocus(view, getActivity());
    }

    @CallSuper
    public void onTouchEvent(MotionEvent event) {
        List<Fragment> childFragments = getChildFragmentManager().getFragments();
        if (childFragments != null) {
            for (Fragment f : childFragments) {
                if (f instanceof BaseJugglerFragment && !f.isDetached()) {
                    ((BaseJugglerFragment) f).onTouchEvent(event);
                }
            }
        }
    }

    @CallSuper
    public void onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            List<Fragment> childFragments = getChildFragmentManager().getFragments();
            if (childFragments != null) {
                for (Fragment f : childFragments) {
                    if (f instanceof BaseJugglerFragment && !f.isDetached()) {
                        ((BaseJugglerFragment) f).onKeyDown(keyCode, e);
                    }
                }
            }
        } else {
            onBackPressed();
        }
    }

    public boolean onBackPressed() {
        List<Fragment> childFragments = getChildFragmentManager().getFragments();
        if (childFragments != null) {
            for (Fragment f : childFragments) {
                if (f instanceof BaseJugglerFragment && !f.isDetached()) {
                    if (((BaseJugglerFragment) f).onBackPressed()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    @CallSuper
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        List<Fragment> childFragments = getChildFragmentManager().getFragments();
        if (childFragments != null) {
            for (Fragment f : childFragments) {
                if (f != null && !f.isDetached())
                    f.onActivityResult(requestCode, resultCode, data);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Nullable
    @Override
    public ViewPager getNestedViewPager() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <V extends View> V findViewById(@Nullable View view, @IdRes int id) throws ClassCastException {
        return view != null? (V) view.findViewById(id) : null;
    }
}

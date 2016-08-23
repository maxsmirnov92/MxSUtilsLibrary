package net.maxsmr.jugglerhelper.fragments.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.maxsmr.commonutils.android.gui.GuiUtils;
import net.maxsmr.jugglerhelper.R;
import net.maxsmr.jugglerhelper.navigation.NavigationMode;

import me.ilich.juggler.gui.JugglerToolbarFragment;

public abstract class BaseJugglerToolbarFragment extends JugglerToolbarFragment {

    @LayoutRes
    protected abstract int getLayoutId();

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutId(), container, false);
        onBindViews(rootView);
        return rootView;
    }

    protected abstract void onBindViews(@NonNull View rootView);
//        ButterKnife.bind(this, rootView);

    protected void initToolbar() {
        View rootView = getView();

        if (rootView == null) {
            throw new IllegalStateException("root view was not created");
        }

        Toolbar toolbar = GuiUtils.findViewById(rootView, getToolbarId());

        if (toolbar == null) {
            throw new IllegalStateException("toolbar was not found");
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getJugglerActivity().onSupportNavigateUp();
            }
        });

        ActionBar actionBar = getJugglerActivity().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.empty);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.show();
        }
    }

    protected void initTitle() {
        CharSequence title = getActivity().getTitle();
        setTitle(title);
    }

    protected void initNavigationMode() {
        setMode(getNavigationMode(), getMenuDrawableId(), getBackDrawableId());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initToolbar();
        initTitle();
        initNavigationMode();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActionBar actionBar = getJugglerActivity().getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    @NonNull
    protected abstract NavigationMode getNavigationMode();

    private void setMode(NavigationMode mode, @DrawableRes int menuDrawableResId, @DrawableRes int backDrawableResId) {

        if (mode != null) {

            Toolbar toolbar = getToolbar();

            if (toolbar == null) {
                throw new RuntimeException("toolbar was not initialized");
            }

            switch (mode) {
                case SANDWICH:
                    setNavigationIconVisible(true);
                    if (menuDrawableResId > 0) {
                        toolbar.setNavigationIcon(menuDrawableResId);
                    }
                    break;
                case BACK:
                    setNavigationIconVisible(true);
                    if (backDrawableResId > 0) {
                        toolbar.setNavigationIcon(backDrawableResId);
                    }
                    break;
                case NONE:
                    setNavigationIconVisible(false);
                    toolbar.setVisibility(View.VISIBLE);
                    setNavigationIconVisible(false);
                    break;
                case INVISIBLE:
                    setNavigationIconVisible(false);
                    toolbar.setVisibility(View.GONE);
                    break;
            }
        }
    }

    @DrawableRes
    protected int getBackDrawableId() {
        return 0;
    }

    @DrawableRes
    protected int getMenuDrawableId() {
        return 0;
    }

    private void setNavigationIconVisible(boolean b) {
        ActionBar actionBar = getJugglerActivity().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(b);
            actionBar.setHomeButtonEnabled(b);
        }
    }

    @Nullable
    protected final CharSequence getTitle() {
        ActionBar actionBar = getJugglerActivity().getSupportActionBar();
        return actionBar != null ? actionBar.getTitle() : null;
    }

    public void setTitle(CharSequence text) {
        Toolbar toolbar = getToolbar();
        if (toolbar == null) {
            throw new RuntimeException("toolbar was not initialized");
        }
        toolbar.setTitle(text);
        ActionBar actionBar = getJugglerActivity().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(text);
        }
    }

    protected void setTitle(@StringRes int textRestId) {
        setTitle(getString(textRestId));
    }

    public void setLogo(Drawable icon) {
        Toolbar toolbar = getToolbar();
        if (toolbar == null) {
            throw new RuntimeException("toolbar was not initialized");
        }
        toolbar.setLogo(icon);
    }

}

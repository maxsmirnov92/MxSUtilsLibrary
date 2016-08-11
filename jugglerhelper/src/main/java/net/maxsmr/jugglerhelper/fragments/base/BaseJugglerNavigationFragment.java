package net.maxsmr.jugglerhelper.fragments.base;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.ilich.juggler.gui.JugglerNavigationFragment;

public abstract class BaseJugglerNavigationFragment extends JugglerNavigationFragment {

    @Nullable
    private Bundle savedInstanceState;

    @Nullable
    public Bundle getSavedInstanceState() {
        return savedInstanceState;
    }

    @LayoutRes
    protected abstract int getLayoutId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.savedInstanceState = savedInstanceState;
        setHasOptionsMenu(false);
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutId(), container, false);
        onBindViews(rootView);
        return rootView;
    }

    protected abstract void onBindViews(@NonNull View rootView);

    @Nullable
    protected String getBaseFontAlias() {
        return null;
    }

    protected void init() {}

    protected void postInit() {}

    protected void unlisten() {}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        postInit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unlisten();
    }

    @Override
    public boolean onBackPressed() {
        return changeDrawerState(false) || super.onBackPressed();
    }

    @Override
    public boolean onUpPressed() {
        return changeDrawerState(true) || super.onUpPressed();
    }

    protected final boolean changeDrawerState(boolean open) {

        if (getActivity() == null) {
            throw new RuntimeException("not attached to activity");
        }

        DrawerLayout drawer = BaseJugglerFragment.findViewById(getActivity().getWindow().getDecorView(), getDrawerLayoutId());

        if (drawer == null) {
            throw new RuntimeException("drawer not found");
        }

        if (!open && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        } else if (open && !drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
            return true;
        }

        return false;
    }

    protected final void revertDrawerState() {
        DrawerLayout drawer = BaseJugglerFragment.findViewById(getActivity().getWindow().getDecorView(), getDrawerLayoutId());

        if (drawer == null) {
            throw new RuntimeException("drawer not found");
        }

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            drawer.openDrawer(GravityCompat.START);
        }
    }


}

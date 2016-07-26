package net.maxsmr.jugglerfragments.activities.base;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.MotionEvent;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.jugglerfragments.fragments.base.BaseJugglerFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import me.ilich.juggler.gui.JugglerActivity;
import me.ilich.juggler.gui.JugglerFragment;


public class BaseJugglerActivity extends JugglerActivity {

    private static final Logger logger = LoggerFactory.getLogger(BaseJugglerActivity.class);

    @Nullable
    @SuppressWarnings("unchecked")
    public <F extends JugglerFragment> F findFragment(Class<F> fragmentClass) {
        FragmentManager fm = getSupportFragmentManager();
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
        FragmentManager fm = getSupportFragmentManager();
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
        FragmentManager fm = getSupportFragmentManager();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        logger.debug("onActivityResult(), this=" + this + ", requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && !fragment.isDetached()) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        logger.debug("onTouchEvent(), event=" + event);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (f instanceof BaseJugglerFragment && !f.isDetached()) {
                    ((BaseJugglerFragment) f).onTouchEvent(event);
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        logger.debug("onKeyDown(), keyCode=" + keyCode + ", event=" + event);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment f : fragments) {
                if (f instanceof BaseJugglerFragment && !f.isDetached()) {
                    ((BaseJugglerFragment) f).onKeyDown(keyCode, event);
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}

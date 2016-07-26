package net.maxsmr.jugglerfragments.navigation;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import java.io.Serializable;

public class NavigationMenuItem implements Serializable {

    public static final int ID_NOT_SET = -1;

    public final int id;

    @StringRes
    public final int titleResId;

    @DrawableRes
    public final int drawableResId;

    public NavigationMenuItem(int id, @StringRes int titleResId, @DrawableRes int drawableResId) {
        this.id = id;
        this.titleResId = titleResId;
        this.drawableResId = drawableResId;
    }
}
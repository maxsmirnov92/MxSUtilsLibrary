package net.maxsmr.commonutils.android.gui.fonts;

import android.content.Context;
import android.graphics.Typeface;
;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;

@Deprecated
public final class FontsHolder {

    private static FontsHolder mInstance;

    public static void initInstance() {
        if (mInstance == null) {
            synchronized (FontsHolder.class) {
                mInstance = new FontsHolder();
            }
        }
    }

    public static FontsHolder getInstance() {
        initInstance();
        return mInstance;
    }

    private FontsHolder() {
    }

    private final Map<String, Typeface> loadedFonts = new HashMap<>();

    @Nullable
    public Typeface getFont(String alias) {
        final Typeface t;
        if (loadedFonts.containsKey(alias)) {
            t = loadedFonts.get(alias);
        } else {
            t = null;
        }
        return t;
    }

    public Typeface loadFont(File file, String alias) {
        Typeface tf = null;
        if (!isEmpty(alias)) {
            if ((tf = loadedFonts.get(alias)) != null) return tf;
            tf = Typeface.createFromFile(file);
            loadedFonts.put(alias, tf);
        }
        return tf;
    }

    public Typeface loadFont(String asset, Context ctx, String alias) {
        Typeface tf;
        if ((tf = loadedFonts.get(alias)) != null) return tf;
        tf = Typeface.createFromAsset(ctx.getAssets(), asset);
        loadedFonts.put(alias, tf);
        return tf;
    }

    public View apply(View view, String tag, boolean withViewTag) {
        if (view != null) {

            if (view instanceof ListView || view instanceof RecyclerView) {
                return view;
            }

            if (view instanceof TextView) {
                String alias = null;
                if (withViewTag) {
                    if (view.getTag() != null) {
                        alias = view.getTag().toString();
                        if (isEmpty(alias)) {
                            alias = tag;
                        }
                    }
                } else {
                    alias = tag;
                }
                Typeface tf = getFont(alias);
                if (tf != null)
                    ((TextView) view).setTypeface(tf);

                return view;
            }

            if (view instanceof ViewGroup) {
                ViewGroup v = (ViewGroup) view;
                for (int i = 0; i < v.getChildCount(); i++)
                    apply(v.getChildAt(i), tag, withViewTag);
            }
        }
        return view;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        loadedFonts.clear();
    }
}
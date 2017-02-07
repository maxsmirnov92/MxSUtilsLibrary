package net.maxsmr.commonutils.android.gui.views.font;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.android.gui.fonts.FontsHolder;

public class FontEditText extends AppCompatEditText implements ITypefaceChangeable {

    public FontEditText(Context context) {
        this(context, null);
    }

    public FontEditText(Context context, AttributeSet attrs) {
        this(context, attrs, context.getResources().getIdentifier("editTextStyle", "attr", "android"));
    }

    public FontEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            setTypefaceByName(getTypeFaceNameFromAttrs(attrs));
        }
    }

    private String getTypeFaceNameFromAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FontEditText, 0, 0);
            try {
                return a.getString(R.styleable.FontEditText_custom_typeface);
            } finally {
                a.recycle();
            }
        }
        return null;
    }

    public void setTypefaceByName(String name) {
        Typeface typeface = FontsHolder.getInstance().getFont(name);
        if (typeface != null) {
            setTypeface(typeface);
        }
    }
}

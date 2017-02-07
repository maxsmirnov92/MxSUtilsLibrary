package net.maxsmr.commonutils.android.gui.views.font;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import net.maxsmr.commonutils.android.gui.fonts.FontsHolder;
import net.maxsmr.commonutils.android.gui.views.PatternedEditText;
public class FontPatternedEditText extends PatternedEditText {

    public FontPatternedEditText(Context context) {
        this(context, null);
    }

    public FontPatternedEditText(Context context, AttributeSet attrs) {
        this(context, attrs, context.getResources().getIdentifier("editTextStyle", "attr", "android"));
    }

    public FontPatternedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            setTypefaceByName(getTypeFaceNameFromAttrs(attrs));
        }
    }

    private String getTypeFaceNameFromAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, net.maxsmr.commonutils.R.styleable.FontEditText, 0, 0);
            try {
                return a.getString(net.maxsmr.commonutils.R.styleable.FontEditText_custom_typeface);
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

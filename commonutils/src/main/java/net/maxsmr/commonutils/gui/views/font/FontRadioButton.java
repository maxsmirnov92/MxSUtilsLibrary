package net.maxsmr.commonutils.gui.views.font;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatRadioButton;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.gui.fonts.FontsHolder;


public class FontRadioButton extends AppCompatRadioButton implements ITypefaceChangeable {

    public FontRadioButton(Context context) {
        this(context, null);
    }

    public FontRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FontRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            setTypefaceByName(getTypeFaceNameFromAttrs(attrs));
        }
    }

    private String getTypeFaceNameFromAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FontRadioButton, 0, 0);
            try {
                return a.getString(R.styleable.FontRadioButton_custom_typeface);
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

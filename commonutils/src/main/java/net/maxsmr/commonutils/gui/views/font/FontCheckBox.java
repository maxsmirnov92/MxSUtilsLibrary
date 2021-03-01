package net.maxsmr.commonutils.gui.views.font;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatCheckBox;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.gui.fonts.FontsHolder;

public class FontCheckBox extends AppCompatCheckBox implements ITypefaceChangeable {

    public FontCheckBox(Context context) {
        this(context, null);
    }

    public FontCheckBox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FontCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            setTypefaceByName(getTypeFaceNameFromAttrs(attrs));
        }
    }

    private String getTypeFaceNameFromAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.FontCheckbox, 0, 0);
            try {
                return a.getString(R.styleable.FontCheckbox_custom_typeface);
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

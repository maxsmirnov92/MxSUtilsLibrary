package net.maxsmr.commonutils.android.gui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import net.maxsmr.commonutils.R;

import java.util.HashSet;
import java.util.Set;


public class PatternedEditText extends AppCompatEditText {

    private static final char DIGIT_CHAR = '0';
    private static final char LETTER_CHAR = 'A';
    private static final char ANY_CHAR = '#';
    private static final Set<Character> PATTERN_CHARACTERS = new HashSet<>();

    static {
        PATTERN_CHARACTERS.add(DIGIT_CHAR);
        PATTERN_CHARACTERS.add(LETTER_CHAR);
        PATTERN_CHARACTERS.add(ANY_CHAR);
    }

    private String realText = "";
    private String pattern;
    private String validation;
    private int alwaysVisibleCharCount;
    private boolean forcingText;
    private int cursorPosition;

    public PatternedEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PatternedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        final Context context = getContext();

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PatternedEditText);
        pattern = array.getString(R.styleable.PatternedEditText_pattern);
        validation = getPatternOnly();
        alwaysVisibleCharCount = array.getInteger(R.styleable.PatternedEditText_alwaysVisibleCharCount, 0);
        array.recycle();

        setLongClickable(false);
//        setCustomSelectionActionModeCallback(new SelectionPreventingCallback());

        final TextWatcher textWatcher = new TextWatcher() {

            String newRealText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (forcingText) {
                    return;
                }
                if (count > after && start > 2) {
                    if (!Character.isDigit(s.charAt(start))) {
                        start--;
                    }
                    cursorPosition = start;
                    int realStartIndex = convertPositionInEditTextToIndex(start);
                    int delDigitsCount = getDigitsCount(start, count);
                    if (realStartIndex + delDigitsCount < realText.length()) {
                        realText = realText.substring(0, realStartIndex) + realText.substring(realStartIndex + count);
                    } else {
                        realText = realText.substring(0, realStartIndex);
                        cursorPosition = -1;
                    }
                } else {
                    cursorPosition = -1;
                }
                newRealText = realText;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (forcingText) {
                    return;
                }
                if (count > 0) {
                    int realStartIndex = convertPositionInEditTextToIndex(start);
                    // add symbol to the end
                    if (start + count == s.length()) {
                        newRealText += s.subSequence(start, start + count);
                    } else { //add symbol to center
                        newRealText = newRealText.substring(0, realStartIndex) + s.subSequence(start, start + count)
                                + newRealText.substring(realStartIndex);
                        cursorPosition = start + count;
                    }

                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (cursorPosition > 0 && cursorPosition < s.length() && !Character.isDigit(s.charAt(cursorPosition))) {
                    cursorPosition++;
                }
                if (forcingText) {
                    return;
                }
                if (isValid(newRealText)) {
                    realText = newRealText;
                }
                updateText();
            }
        };
        addTextChangedListener(textWatcher);
        updateText();
    }

    public void setAlwaysVisibleCharCount(int alwaysVisibleCharCount) {
        this.alwaysVisibleCharCount = alwaysVisibleCharCount;
        updateText();
    }


    private int convertPositionInEditTextToIndex(int start) {
        String text = getText().toString();
        int index = 0;
        for (int i = alwaysVisibleCharCount; i < start; i++) {
            if (Character.isDigit(text.charAt(i))) {
                index++;
            }
        }
        return index;
    }


    private int getDigitsCount(int start, int count) {
        String text = getText().toString();
        int length = text.length();
        int quantity = 0;
        for (int i = start; i < text.length() && i < start + count; i++) {
            if (Character.isDigit(text.charAt(i))) {
                quantity++;
            }
        }
        return quantity;
    }

    public void setRealText(String text) {
        realText = text;
        updateText();
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private static class SelectionPreventingCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }

    private void updateText() {
        forcingText = true;
        final String text = realTextToPattern();
        setText(text);
        if (cursorPosition > 0) {
            setSelection(cursorPosition);
        } else {
            setSelection(text.length());
        }

        forcingText = false;
    }

    private String getPatternOnly() {
        final int length = pattern.length();
        final StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final char patternChar = pattern.charAt(i);
            if (isPatternChar(patternChar)) {
                result.append(patternChar);
            }
        }
        return result.toString();
    }

    private boolean isValid(String text) {
        if (text.length() > validation.length()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (validation.charAt(i)) {
                case DIGIT_CHAR:
                    if (!Character.isDigit(ch)) {
                        return false;
                    }
                    break;
                case LETTER_CHAR:
                    if (!Character.isLetter(ch)) {
                        return false;
                    }
                    break;
            }
        }
        return true;
    }

    private String realTextToPattern() {
        int offset = 0;
        final int length = realText.length();
        final StringBuilder result = new StringBuilder(length);
        int i = 0;
        while (i < length) {
            final char charWithOffset = pattern.charAt(i + offset);
            if (isPatternChar(charWithOffset)) {
                result.append(realText.charAt(i));
                i++;
            } else {
                result.append(charWithOffset);
                offset++;
            }
        }
        int resultLength = result.length();
        if (resultLength > alwaysVisibleCharCount && resultLength < pattern.length()
                && !Character.isDigit(pattern.charAt(i + offset))) {
            result.append(pattern.charAt(i + offset));
        }
        if (result.length() == 0) {
            return pattern.substring(0, alwaysVisibleCharCount);
        }
        return result.toString();
    }

    private boolean isPatternChar(char ch) {
        return PATTERN_CHARACTERS.contains(ch);
    }

    public String getRealText() {
        return realText;
    }

    public boolean isFilled() {
        return validation.length() == realText.length();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.realText = this.realText;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.realText = ss.realText;
        updateText();
    }

    static class SavedState extends BaseSavedState {
        String realText;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.realText = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(this.realText);
        }

        public static final Creator<SavedState> CREATOR =
                new Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}

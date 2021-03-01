package net.maxsmr.commonutils.gui.views;

import android.text.method.ReplacementTransformationMethod;

public class WordBreakTransformationMethod extends ReplacementTransformationMethod {

    private static WordBreakTransformationMethod instance;

    public static WordBreakTransformationMethod getInstance() {
        if (instance == null) {
            instance = new WordBreakTransformationMethod();
        }
        return instance;
    }

    private WordBreakTransformationMethod() {
    }

    private static char[] dash = new char[]{'-', '\u2011'};
    private static char[] space = new char[]{' ', '\u00A0'};

    private static char[] original = new char[]{dash[0], space[0]};
    private static char[] replacement = new char[]{dash[1], space[1]};

    @Override
    protected char[] getOriginal() {
        return original;
    }

    @Override
    protected char[] getReplacement() {
        return replacement;
    }
}
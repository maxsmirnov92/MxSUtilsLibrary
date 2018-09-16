package net.maxsmr.commonutils.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import net.maxsmr.commonutils.R;

public class StringUtils {

    private static List<CharacterMap> replaceLatinCyrillicAlphabet = new ArrayList<>();

    static {
        replaceLatinCyrillicAlphabet.add(new CharacterMap('a', 'а'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('b', 'б'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('c', 'ц'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('d', 'д'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('e', 'е'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('f', 'ф'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('g', 'г'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('h', 'х'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('i', 'и'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('j', 'ж'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('k', 'к'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('l', 'л'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('m', 'м'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('n', 'н'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('o', 'о'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('p', 'п'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('q', 'к'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('r', 'р'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('s', 'с'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('t', 'т'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('u', 'у'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('v', 'в'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('w', 'в'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('x', 'х'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('y', 'у'));
        replaceLatinCyrillicAlphabet.add(new CharacterMap('z', 'з'));
    }

    public static boolean isEmpty(@Nullable CharSequence s) {
        return TextUtils.isEmpty(s) || "null".equalsIgnoreCase(s.toString());
    }

    public static boolean isNoData(CharSequence s, @NonNull Context ctx) {
        return isEmpty(s) || ctx.getString(R.string.no_data).equalsIgnoreCase(s.toString());
    }

    @Nullable
    public static String changeCaseFirstLatter(@Nullable CharSequence s, boolean upper) {
        String result;
        if (TextUtils.isEmpty(s)) {
            result = null;
        } else {
            result = s.toString();
            if (s.length() == 1) {
                result = upper? result.toUpperCase() : result.toLowerCase();
            } else {
                result = (upper? result.substring(0, 1).toUpperCase() : result.substring(0, 1).toLowerCase()) + result.substring(1);
            }
        }
        return result;
//        String first = parts[i].substring(0, 1);
//        parts[i] = parts[i].replace(first, first.toUpperCase());
    }

    @NonNull
    public static String insertAt(@NonNull CharSequence target, int index, @NonNull CharSequence what) {

        if (index < 0 || index >= target.length()) {
            throw new IllegalArgumentException("incorrect index: " + index);
        }

        return target.toString().substring(0, index) + what + target.toString().substring(index, target.length());
    }

    public static int indexOf(@NonNull CharSequence s, char c) {
        for (int index = 0; index < s.length(); index++) {
            if (s.charAt(index) == c) {
                return index;
            }
        }
        return -1;
    }

    @NonNull
    public static CharSequence removeNonDigits(@NonNull CharSequence s) {
        StringBuilder format = new StringBuilder(s);
        for (int i = 0; i < format.length(); )
            if (!Character.isDigit(format.charAt(i)))
                format.deleteCharAt(i);
            else i++;
        return format;
    }

    @NonNull
    public static String replace(@NonNull CharSequence s, int start, int end, @NonNull CharSequence replacement) {

        if (start < 0)
            throw new StringIndexOutOfBoundsException("start < 0");
        if (start > end)
            throw new StringIndexOutOfBoundsException("start > end");
        if (end > s.length())
            throw new StringIndexOutOfBoundsException("end > length");

        StringBuilder sb = new StringBuilder(s);
        sb.replace(start, end, replacement.toString());
        return sb.toString();
    }

    @NonNull
    public static String delete(@NonNull CharSequence s, int start, int end) {

        if (start < 0)
            throw new StringIndexOutOfBoundsException("start < 0");
        if (start > end)
            throw new StringIndexOutOfBoundsException("start > end");
        if (end > s.length())
            throw new StringIndexOutOfBoundsException("end > length");

        StringBuilder sb = new StringBuilder(s);
        sb.delete(start, end);
        return sb.toString();
    }

    @NonNull
    public static String deleteCharAt(@NonNull CharSequence s, int index) {

        if (index < 0 || index >= s.length())
            throw new StringIndexOutOfBoundsException("incorrect index: " + index);

        StringBuilder sb = new StringBuilder(s);
        sb.deleteCharAt(index);
        return sb.toString();
    }

    public static String getStubValue(@Nullable String value, Context ctx) {
        return getStubValueWithAppend(value, null, ctx);
    }

    public static String getStubValue(int value, Context ctx) {
        return getStubValueWithAppend(value, null, ctx);
    }

    public static String getStubValueWithAppend(int value, @Nullable String appendWhat, Context ctx) {
        return getStubValueWithAppend(value != 0 ? String.valueOf(value) : null, appendWhat, ctx);
    }

    public static String getStubValueWithAppend(@Nullable String value, @Nullable String appendWhat, Context ctx) {
        return !isEmpty(value) ? (!isEmpty(appendWhat) ? value + " " + appendWhat : value) : ctx.getString(R.string.no_data);
    }

    public static class CharacterMap {

        @Nullable
        public final Character leftCh;

        @Nullable
        public final Character rightCh;

        public CharacterMap(@Nullable Character leftCh, @Nullable Character rightCh) {
            this.leftCh = leftCh;
            this.rightCh = rightCh;
        }
    }

    @NonNull
    private static List<CharacterMap> lowerCaseAlphabet(@NonNull List<CharacterMap> alphabet) {
        List<CharacterMap> newAlphabet = new ArrayList<>();
        for (CharacterMap map : alphabet) {
            if (map != null) {
                newAlphabet.add(new CharacterMap(map.leftCh != null ? Character.toLowerCase(map.leftCh) : null, map.rightCh != null ? Character.toLowerCase(map.rightCh) : null));
            }
        }
        return newAlphabet;
    }

    @NonNull
    private static List<CharacterMap> upperCaseAlphabet(@NonNull List<CharacterMap> alphabet) {
        List<CharacterMap> newAlphabet = new ArrayList<>();
        for (CharacterMap map : alphabet) {
            if (map != null) {
                newAlphabet.add(new CharacterMap(map.leftCh != null ? Character.toUpperCase(map.leftCh) : null, map.rightCh != null ? Character.toUpperCase(map.rightCh) : null));
            }
        }
        return newAlphabet;
    }

    @NonNull
    public static String trim(CharSequence cs) {
        return trim(cs, true, true);
    }

    @NonNull
    public static String trim(CharSequence cs, boolean fromStart, boolean fromEnd) {
        return trim(cs, CompareUtils.Condition.LESS_OR_EQUAL, ' ', fromStart, fromEnd);
    }

    @NonNull
    public static String trim(CharSequence cs, CompareUtils.Condition condition, char byChar, boolean fromStart, boolean fromEnd) {

        if (cs == null) {
            return "";
        }

        String str = cs.toString();

        int len = str.length();
        int st = 0;

        if (fromStart) {
            while ((st < len) && (condition.apply(str.charAt(st), byChar, false))) {
                st++;
            }
        }
        if (fromEnd) {
            while ((st < len) && (condition.apply(str.charAt(len - 1), byChar, false))) {
                len--;
            }
        }
        return ((st > 0) || (len < str.length())) ? str.substring(st, len) : str;
    }

    @Nullable
    private static Character findCharByLeft(@NonNull List<CharacterMap> alphabet, Character c, boolean ignoreCase) {
        for (CharacterMap map : alphabet) {
            if (CompareUtils.charsEqual(c, map.leftCh, ignoreCase))
                return map.rightCh;
        }
        return null;
    }

    @Nullable
    private static Character findCharByRight(@NonNull List<CharacterMap> alphabet, Character c, boolean ignoreCase) {
        for (CharacterMap map : alphabet) {
            if (CompareUtils.charsEqual(c, map.rightCh, ignoreCase))
                return map.leftCh;
        }
        return null;
    }

    @Nullable
    public static CharSequence replaceChars(@NonNull List<CharacterMap> alphabet, @Nullable CharSequence sequence, @NonNull ReplaceDirection direction, boolean ignoreCase) {
        if (sequence != null) {
            char[] source = new char[sequence.length()];
            for (int i = 0; i < sequence.length(); i++) {
                char ch = sequence.charAt(i);
                Character replacement = direction == ReplaceDirection.LEFT_RIGHT ? findCharByLeft(alphabet, ch, ignoreCase) : findCharByRight(alphabet, ch, ignoreCase);
                source[i] = replacement != null ? replacement : ch;
            }
            return String.copyValueOf(source);
        }
        return null;
    }

    @NonNull
    public static String appendOrReplaceChar(CharSequence source, Character what, String to, boolean ignoreCase, boolean appendOrReplace) {

        if (TextUtils.isEmpty(source) || TextUtils.isEmpty(to)) {
            return "";
        }

        StringBuilder newStr = new StringBuilder();

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (CompareUtils.charsEqual(c, what, ignoreCase)) {
                if (appendOrReplace) {
                    newStr.append(c);
                    newStr.append(to);
                } else {
                    newStr.append(to);
                }
            } else {
                newStr.append(c);
            }
        }

        return newStr.toString();
    }

    @Nullable
    private static CharSequence replaceByLatinCyrillicAlphabet(CharSequence sequence, boolean ignoreCase, @NonNull StringUtils.ReplaceDirection direction) {
        return replaceChars(replaceLatinCyrillicAlphabet, sequence, direction, ignoreCase);
    }

    public enum ReplaceDirection {
        LEFT_RIGHT, RIGHT_LEFT
    }

}

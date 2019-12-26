package net.maxsmr.commonutils.data;

import net.maxsmr.commonutils.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static net.maxsmr.commonutils.data.SymbolConstKt.EMPTY_STRING;

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
        return isEmpty(s, false);
    }
    
    public static boolean isEmpty(@Nullable CharSequence s, boolean shouldCheckNullString) {
        return (s == null || s.equals(EMPTY_STRING)) || shouldCheckNullString && "null".equalsIgnoreCase(s.toString());
    }

    public static boolean isEmptyData(CharSequence s, @NotNull StringsProvider stringsProvider) {
        return isEmpty(s) || stringsProvider.getString(R.string.empty_data).equalsIgnoreCase(s.toString());
    }

    // Copied from TextUtils
    public static String join(@NotNull CharSequence delimiter, @NotNull Object[] tokens) {
        final int length = tokens.length;
        if (length == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(tokens[0]);
        for (int i = 1; i < length; i++) {
            sb.append(delimiter);
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    // Copied from TextUtils
    public static String join(@NotNull CharSequence delimiter, @NotNull Iterable tokens) {
        final Iterator<?> it = tokens.iterator();
        if (!it.hasNext()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(it.next());
        while (it.hasNext()) {
            sb.append(delimiter);
            sb.append(it.next());
        }
        return sb.toString();
    }

    // Copied from TextUtils
    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return EMPTY_STRING_ARRAY;
        } else {
            return text.split(expression, -1);
        }
    }

    @NotNull
    public static String changeCaseFirstLatter(@Nullable CharSequence s, boolean upper) {
        String result = EMPTY_STRING;
        if (!isEmpty(s)) {
            result = s.toString();
            if (s.length() == 1) {
                result = upper? result.toUpperCase(Locale.getDefault()) : result.toLowerCase(Locale.getDefault());
            } else {
                result = (upper? result.substring(0, 1).toUpperCase(Locale.getDefault())
                        : result.substring(0, 1).toLowerCase(Locale.getDefault()))
                        + result.substring(1);
            }
        }
        return result;
    }

    @NotNull
    public static String insertAt(@NotNull CharSequence target, int index, @NotNull CharSequence what) {

        if (index < 0 || index >= target.length()) {
            throw new IllegalArgumentException("incorrect index: " + index);
        }

        return target.toString().substring(0, index) + what + target.toString().substring(index, target.length());
    }

    public static int indexOf(@NotNull CharSequence s, char c) {
        for (int index = 0; index < s.length(); index++) {
            if (s.charAt(index) == c) {
                return index;
            }
        }
        return -1;
    }

    @NotNull
    public static CharSequence removeNonDigits(@NotNull CharSequence s) {
        StringBuilder format = new StringBuilder(s);
        for (int i = 0; i < format.length(); )
            if (!Character.isDigit(format.charAt(i)))
                format.deleteCharAt(i);
            else i++;
        return format;
    }

    @NotNull
    public static String replace(@NotNull CharSequence s, int start, int end, @NotNull CharSequence replacement) {

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

    @NotNull
    public static String delete(@NotNull CharSequence s, int start, int end) {

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

    @NotNull
    public static String deleteCharAt(@NotNull CharSequence s, int index) {

        if (index < 0 || index >= s.length())
            throw new StringIndexOutOfBoundsException("incorrect index: " + index);

        StringBuilder sb = new StringBuilder(s);
        sb.deleteCharAt(index);
        return sb.toString();
    }

    public static int strToInt(String value) {
        int result = 0;
        try {
            result = Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public static String getStubValue(@Nullable String value, StringsProvider stringsProvider) {
        return getStubValueWithAppend(value, null, stringsProvider);
    }

    public static String getStubValue(int value, StringsProvider stringsProvider) {
        return getStubValueWithAppend(value, null, stringsProvider);
    }

    public static String getStubValueWithAppend(int value, @Nullable String appendWhat, StringsProvider stringsProvider) {
        return getStubValueWithAppend(value != 0 ? String.valueOf(value) : null, appendWhat, stringsProvider);
    }

    public static String getStubValueWithAppend(@Nullable String value, @Nullable String appendWhat, StringsProvider stringsProvider) {
        return !isEmpty(value) ? (!isEmpty(appendWhat) ? value + " " + appendWhat : value) : stringsProvider.getString(R.string.empty_data);
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

    @NotNull
    private static List<CharacterMap> lowerCaseAlphabet(@NotNull List<CharacterMap> alphabet) {
        List<CharacterMap> newAlphabet = new ArrayList<>();
        for (CharacterMap map : alphabet) {
            if (map != null) {
                newAlphabet.add(new CharacterMap(map.leftCh != null ? Character.toLowerCase(map.leftCh) : null, map.rightCh != null ? Character.toLowerCase(map.rightCh) : null));
            }
        }
        return newAlphabet;
    }

    @NotNull
    private static List<CharacterMap> upperCaseAlphabet(@NotNull List<CharacterMap> alphabet) {
        List<CharacterMap> newAlphabet = new ArrayList<>();
        for (CharacterMap map : alphabet) {
            if (map != null) {
                newAlphabet.add(new CharacterMap(map.leftCh != null ? Character.toUpperCase(map.leftCh) : null, map.rightCh != null ? Character.toUpperCase(map.rightCh) : null));
            }
        }
        return newAlphabet;
    }

    @NotNull
    public static String trim(CharSequence cs) {
        return trim(cs, true, true);
    }

    @NotNull
    public static String trim(CharSequence cs, boolean fromStart, boolean fromEnd) {
        return trim(cs, CompareUtils.Condition.LESS_OR_EQUAL, ' ', fromStart, fromEnd);
    }

    @NotNull
    public static String trim(CharSequence cs, CompareUtils.Condition condition, char byChar, boolean fromStart, boolean fromEnd) {

        if (cs == null) {
            return EMPTY_STRING;
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
    private static Character findCharByLeft(@NotNull List<CharacterMap> alphabet, Character c, boolean ignoreCase) {
        for (CharacterMap map : alphabet) {
            if (CompareUtils.charsEqual(c, map.leftCh, ignoreCase))
                return map.rightCh;
        }
        return null;
    }

    @Nullable
    private static Character findCharByRight(@NotNull List<CharacterMap> alphabet, Character c, boolean ignoreCase) {
        for (CharacterMap map : alphabet) {
            if (CompareUtils.charsEqual(c, map.rightCh, ignoreCase))
                return map.leftCh;
        }
        return null;
    }

    @Nullable
    public static CharSequence replaceChars(@NotNull List<CharacterMap> alphabet, @Nullable CharSequence sequence, @NotNull ReplaceDirection direction, boolean ignoreCase) {
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

    @Nullable
    public static String removeChars(@Nullable String text, String... characters) {
        if (characters != null) {
            if (!isEmpty(text)) {
                for (String c : characters) {
                    text = text.replace(c, EMPTY_STRING);
                }
            }
        }
        return text;
    }

    @NotNull
    public static String appendOrReplaceChar(CharSequence source, Character what, String to, boolean ignoreCase, boolean appendOrReplace) {

        if (isEmpty(source) || isEmpty(to)) {
            return EMPTY_STRING;
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
    private static CharSequence replaceByLatinCyrillicAlphabet(CharSequence sequence, boolean ignoreCase, @NotNull StringUtils.ReplaceDirection direction) {
        return replaceChars(replaceLatinCyrillicAlphabet, sequence, direction, ignoreCase);
    }

    public enum ReplaceDirection {
        LEFT_RIGHT, RIGHT_LEFT
    }

    public interface StringsProvider {

        String getString(int resId);
    }
}

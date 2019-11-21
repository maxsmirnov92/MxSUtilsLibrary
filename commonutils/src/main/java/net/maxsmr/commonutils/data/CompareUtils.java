package net.maxsmr.commonutils.data;

import android.os.Bundle;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.AUTO;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.AUTO_IGNORE_CASE;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.CONTAINS;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.CONTAINS_IGNORE_CASE;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.END_WITH;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.END_WITH_IGNORE_CASE;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.EQUALS;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.EQUALS_IGNORE_CASE;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.STARTS_WITH;
import static net.maxsmr.commonutils.data.CompareUtils.MatchStringOption.STARTS_WITH_IGNORE_CASE;
import static net.maxsmr.commonutils.data.SymbolConstKt.EMPTY_STRING;

public final class CompareUtils {

    public CompareUtils() {
        throw new AssertionError("no instances.");
    }

    public static boolean objectsEqual(@Nullable Object one, @Nullable Object another) {
        return one != null ? one.equals(another) : another == null;
    }

    public static boolean charsEqual(@Nullable Character one, @Nullable Character another, boolean ignoreCase) {
        if (ignoreCase) {
            one = one != null ? Character.toUpperCase(one) : null;
            another = another != null ? Character.toUpperCase(another) : null;
        }
        return one != null ? one.equals(another) : another == null;
    }

    public static boolean stringsEqual(@Nullable String one, @Nullable String another, boolean ignoreCase) {
        return one != null ? (!ignoreCase ? one.equals(another) : one.equalsIgnoreCase(another)) : another == null;
    }

    public static int compareForNull(@Nullable Object lhs, @Nullable Object rhs, boolean ascending) {
        return ascending ?
                lhs != null ? (rhs != null ? 0 : 1) : (rhs == null ? 0 : -1) :
                lhs != null ? (rhs != null ? 0 : -1) : (rhs == null ? 0 : 1);
    }

    public static <C extends Comparable<C>> int compareObjects(@Nullable C one, @Nullable C another, boolean ascending) {
        return one != null ? (another != null ? ascending ? one.compareTo(another) : another.compareTo(one) : 1) : another == null ? 0 : -1;
    }

    public static int compareInts(@Nullable Integer one, @Nullable Integer another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : another == null ? 0 : -1;
    }

    public static int compareLongs(@Nullable Long one, @Nullable Long another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : another == null ? 0 : -1;
    }

    public static int compareFloats(@Nullable Float one, @Nullable Float another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : another == null ? 0 : -1;
    }

    public static int compareDouble(@Nullable Double one, @Nullable Double another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : another == null ? 0 : -1;
    }

    public static int compareChars(@Nullable Character one, @Nullable Character another, boolean ascending, boolean ignoreCase) {
        if (charsEqual(one, another, ignoreCase)) {
            return 0;
        }
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : another == null ? 0 : -1;
    }

    public static int compareStrings(@Nullable String one, @Nullable String another, boolean ascending, boolean ignoreCase) {
        if (stringsEqual(one, another, ignoreCase)) {
            return 0;
        }
        return one != null ? another != null ? !ascending ? one.compareTo(another) : another.compareTo(one) : 1 : -1;
    }

    public static int compareDates(@Nullable Date one, @Nullable Date another, boolean ascending) {
        return compareLongs(one != null ? one.getTime() : 0, another != null ? another.getTime() : 0, ascending);
    }

    public static boolean bundleEquals(@Nullable Bundle one, @Nullable Bundle two) {

        if (one == null && two == null)
            return false;

        if ((one != null ? one.size() : 0) != (two != null ? two.size() : 0))
            return false;

        if (one != null) {
            if (two != null) {
                for (String key : one.keySet()) {
                    Object valueOne = one.get(key);
                    Object valueTwo = two.get(key);
                    if (valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                            !bundleEquals((Bundle) valueOne, (Bundle) valueTwo)) {
                        return false;
                    } else if (valueOne == null) {
                        if (valueTwo != null || !two.containsKey(key))
                            return false;
                    } else if (!valueOne.equals(valueTwo))
                        return false;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * то же самое, что и расширенная версия {@linkplain CompareUtils#stringsMatch},
     * но с дефолтным флагом и разделителями
     */
    public static boolean stringsMatch(@Nullable CharSequence initialText, @Nullable CharSequence matchText) {
        return stringsMatch(initialText, matchText, MatchStringOption.AUTO_IGNORE_CASE.flag);
    }

    /**
     * @param initialText строка, в которой ищутся вхождения
     * @param matchText   сопостовляемая строка
     * @param matchFlags  флаги, собранные из {@linkplain MatchStringOption} для выбора опций поиска соответствий
     * @param separators  опциональные разделители для разбивки на подстроки в режиме [MatchStringOption.AUTO] или [MatchStringOption.AUTO_IGNORE_CASE]
     * @return true если соответствие найдено, false - в противном случае
     */
    public static boolean stringsMatch(@Nullable CharSequence initialText, @Nullable CharSequence matchText, int matchFlags,
                                       String... separators) {

        if (initialText == null) {
            initialText = EMPTY_STRING;
        }

        if (matchText == null) {
            matchText = EMPTY_STRING;
        }

        String one = initialText.toString();
        String another = matchText.toString();

        boolean match = false;

        if (CompareUtils.MatchStringOption.contains(EQUALS, matchFlags)) {
            if (one.equals(another)) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(EQUALS_IGNORE_CASE, matchFlags)) {
            if (one.equalsIgnoreCase(another)) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(CONTAINS, matchFlags)) {
            if (one.contains(another)) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(CONTAINS_IGNORE_CASE, matchFlags)) {
            if (one.toLowerCase().contains(another.toLowerCase())) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(STARTS_WITH, matchFlags)) {
            if (one.startsWith(another)) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(STARTS_WITH_IGNORE_CASE, matchFlags)) {
            if (one.toLowerCase().startsWith(another.toLowerCase())) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(END_WITH, matchFlags)) {
            if (one.endsWith(another)) {
                match = true;
            }
        }
        if (!match && CompareUtils.MatchStringOption.contains(END_WITH_IGNORE_CASE, matchFlags)) {
            if (one.toLowerCase().endsWith(another.toLowerCase())) {
                match = true;
            }
        }
        if (!match && (CompareUtils.MatchStringOption.contains(AUTO, matchFlags) || CompareUtils.MatchStringOption.contains(AUTO_IGNORE_CASE, matchFlags))) {
            if (!TextUtils.isEmpty(one)) {
                final boolean isIgnoreCase = CompareUtils.MatchStringOption.contains(AUTO_IGNORE_CASE, matchFlags);
                one = isIgnoreCase ? one.toLowerCase().trim() : one;
                another = isIgnoreCase ? another.toLowerCase().trim() : another;
                if (stringsEqual(one, another, false)) {
                    match = true;
                } else {
                    final String[] parts = one.split("[" + (separators != null && separators.length > 0 ?
                            TextUtils.join(EMPTY_STRING, separators) : " ") + "]+");
                    if (parts.length > 0) {
                        for (String word : parts) {
                            if (isIgnoreCase) {
                                word = word.toLowerCase();
                            }
                            if (!CompareUtils.MatchStringOption.containsAny(matchFlags, CompareUtils.MatchStringOption.valuesExceptOf(AUTO, AUTO_IGNORE_CASE).toArray(new CompareUtils.MatchStringOption[]{}))) {
                                if (word.startsWith(another) || word.endsWith(another)) {
                                    match = true;
                                    break;
                                }
                            } else if (parts.length > 1) {
                                if (stringsMatch(word, another, CompareUtils.MatchStringOption.resetFlags(matchFlags, AUTO, AUTO_IGNORE_CASE))) {
                                    match = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return match;
    }

    public enum MatchStringOption {

        AUTO(1), AUTO_IGNORE_CASE(1 << 1), EQUALS(1 << 2), EQUALS_IGNORE_CASE(1 << 3), CONTAINS(1 << 4), CONTAINS_IGNORE_CASE(1 << 5), STARTS_WITH(1 << 6), STARTS_WITH_IGNORE_CASE(1 << 7), END_WITH(1 << 8), END_WITH_IGNORE_CASE(1 << 9);

        public final int flag;

        MatchStringOption(int flag) {
            this.flag = flag;
        }

        public static boolean contains(@NotNull MatchStringOption option, int flags) {
            return (flags & option.flag) == option.flag;
        }

        public static boolean containsAny(int flags, MatchStringOption... options) {
            boolean contains = false;
            if (options != null) {
                for (MatchStringOption o : options) {
                    if (o != null && contains(o, flags)) {
                        contains = true;
                        break;
                    }
                }
            }
            return contains;
        }

        public static int setFlag(int flags, @NotNull MatchStringOption option) {
            return flags | option.flag;
        }

        public static int setFlags(int flags, MatchStringOption... options) {
            if (options != null) {
                for (MatchStringOption o : options) {
                    if (o != null) {
                        flags = setFlag(flags, o);
                    }
                }
            }
            return flags;
        }

        public static int resetFlag(int flags, @NotNull MatchStringOption option) {
            return flags & ~option.flag;
        }

        public static int resetFlags(int flags, MatchStringOption... options) {
            if (options != null) {
                for (MatchStringOption o : options) {
                    if (o != null) {
                        flags = resetFlag(flags, o);
                    }
                }
            }
            return flags;
        }


        public static Set<MatchStringOption> valuesExceptOf(MatchStringOption... exclude) {
            Set<MatchStringOption> result = new LinkedHashSet<>();
            result.addAll(Arrays.asList(values()));
            if (exclude != null) {
                result.removeAll(Arrays.asList(exclude));
            }
            return result;
        }
    }

    public enum Condition {

        LESS, LESS_OR_EQUAL, EQUAL, MORE, MORE_OR_EQUAL;

        public boolean apply(Character one, Character another, boolean ignoreCase) {
            return fromResult(compareChars(one, another, true, ignoreCase), this);
        }

        public boolean apply(Number one, Number another) {
            return fromResult(compareDouble(one != null ? one.doubleValue() : null, another != null ? another.doubleValue() : null, true), this);
        }

        public static boolean fromResult(int result, Condition c) {
            switch (c) {
                case LESS:
                    return result < 0;
                case LESS_OR_EQUAL:
                    return result <= 0;
                case EQUAL:
                    return result == 0;
                case MORE:
                    return result > 0;
                case MORE_OR_EQUAL:
                    return result >= 0;
                default:
                    throw new IllegalArgumentException("Unknown " + Condition.class.getSimpleName() + ": " + c);
            }
        }
    }

}

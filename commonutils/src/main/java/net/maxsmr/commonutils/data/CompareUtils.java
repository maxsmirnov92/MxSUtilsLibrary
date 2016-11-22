package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Date;

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
        if (one == null || another == null) {
            return one == null ? (another == null ? 0 : -1) : 1;
        }
        return ascending ? one.compareTo(another) : another.compareTo(one);
    }

    public static int compareInts(@Nullable Integer one, @Nullable Integer another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : -1;
    }

    public static int compareLongs(@Nullable Long one, @Nullable Long another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : -1;
    }

    public static int compareFloats(@Nullable Float one, @Nullable Float another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : -1;
    }

    public static int compareDouble(@Nullable Double one, @Nullable Double another, boolean ascending) {
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : -1;
    }

    public static int compareChars(@Nullable Character one, @Nullable Character another, boolean ascending, boolean ignoreCase) {
        if (charsEqual(one, another, ignoreCase)) {
            return 0;
        }
        return one != null ? (another != null ? !ascending ? (int) Math.signum(another - one) : (int) Math.signum(one - another) : 1) : -1;
    }

    public static int compareStrings(@Nullable String one, @Nullable String another, boolean ascending, boolean ignoreCase) {
        if (stringsEqual(one, another, ignoreCase)) {
            return 0;
        }
        return one != null ? (another != null ? !ascending ? one.compareTo(another) : another.compareTo(one) : 1) : -1;
    }

    public static int compareDates(@Nullable Date one, @Nullable Date another, boolean ascending) {
        return compareLongs(one != null ? one.getTime() : 0, another != null ? another.getTime() : 0, ascending);
    }

    public static boolean stringMatches(@NonNull String one, @NonNull String another, int matchFlags) {

        boolean match = false;

        if (MatchStringOption.contains(EQUALS, matchFlags)) {
            if (one.equals(another)) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(EQUALS_IGNORE_CASE, matchFlags)) {
            if (one.equalsIgnoreCase(another)) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(CONTAINS, matchFlags)) {
            if (one.contains(another)) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(CONTAINS_IGNORE_CASE, matchFlags)) {
            if (one.toLowerCase().contains(another.toLowerCase())) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(STARTS_WITH, matchFlags)) {
            if (one.startsWith(another)) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(STARTS_WITH_IGNORE_CASE, matchFlags)) {
            if (one.toLowerCase().startsWith(another.toLowerCase())) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(END_WITH, matchFlags)) {
            if (one.startsWith(another)) {
                match = true;
            }
        }
        if (!match && MatchStringOption.contains(END_WITH_IGNORE_CASE, matchFlags)) {
            if (one.toLowerCase().startsWith(another.toLowerCase())) {
                match = true;
            }
        }
        if (!match && (MatchStringOption.contains(AUTO, matchFlags) || MatchStringOption.contains(AUTO_IGNORE_CASE, matchFlags))) {
            if (!TextUtils.isEmpty(one)) {
                String valueText = one.toLowerCase().trim();
                if (stringsEqual(valueText, another, MatchStringOption.contains(AUTO_IGNORE_CASE, matchFlags)) || valueText.startsWith(another)) { // endsWith()
                    match = true;
                } else {
                    final String[] parts = valueText.split("[ ]+");
                    if (parts.length > 0) {
                        for (String word : parts) {
                            if (word.startsWith(another)) {
                                match = true;
                                break;
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

        public static boolean contains(@NonNull MatchStringOption option, int flags) {
            return (flags & option.flag) == option.flag;
        }

    }

}

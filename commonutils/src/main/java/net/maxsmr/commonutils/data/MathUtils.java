package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class MathUtils {

    public MathUtils() {
        throw new AssertionError("no instances.");
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public static double round(double value, int precision) {
        if (precision < 0) {
            throw new IllegalArgumentException("incorrect precision: "+ precision);
        }
        double delimiter = 1d;
        for (int i = 0; i < precision; i++) {
            delimiter *= 10;
        }
        return (double) Math.round(value * delimiter) / delimiter;
    }

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // NOTE: This will (intentionally) not run as written so that folks
        // copy-pasting have to think about how to initialize their
        // Random instance.  Initialization of the Random instance is outside
        // the main scope of the question, but some decent options are to have
        // a field that is initialized once and then re-used as needed or to
        // use ThreadLocalRandom (if using at least Java 1.7).
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive

        return rand.nextInt((max - min) + 1) + min;
    }

    public static long randLong(long min, long max) {
        return (new Random().nextLong() % (max - min)) + min;
    }

    @Nullable
    public static Integer generateNumber(Collection<? extends Number> excludeNumbers, int minValue, int maxValue) {

        if (minValue > maxValue) {
            throw new IllegalArgumentException("minValue (" + minValue + ") > maxValue (" + maxValue + ")");
        }

        int rangeSize = maxValue - minValue;
        Set<Integer> checkedCodes = new LinkedHashSet<>();

        boolean found = true;
        int newCode = minValue;

        if (excludeNumbers != null && !excludeNumbers.isEmpty()) {
            excludeNumbers = new ArrayList<>(excludeNumbers);
            found = false;
            int iterations = 0;
            List<? extends Number> excludeNumbersCopy = new ArrayList<>(excludeNumbers);
            for (int i = 0; i < excludeNumbersCopy.size() && iterations <= rangeSize * 2; i++) {
                Number code = excludeNumbersCopy.get(i);
                if (code != null && code.intValue() == newCode) {
                    newCode = randInt(minValue, maxValue);
                    if (!checkedCodes.contains(newCode)) {
                        checkedCodes.add(newCode);
                        i = 0;
                        iterations++;
                    }
                } else {
                    if (i == excludeNumbers.size() - 1) {
                        found = true;
                    }
                }
            }
        }

        return found ? newCode : null;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <N extends Number> N valueOf(String str, @NonNull Class<N> numberClass) {
        if (numberClass.isAssignableFrom(Byte.class)) {
            try {
                return (N) Byte.valueOf(str);
            } catch (NumberFormatException e) {
                return (N) Byte.valueOf("0");
            }
        } else if (numberClass.isAssignableFrom(Integer.class)) {
            try {
                return (N) Integer.valueOf(str);
            } catch (NumberFormatException e) {
                return (N) Integer.valueOf(0);
            }
        } else if (numberClass.isAssignableFrom(Long.class)) {
            try {
                return (N) Long.valueOf(str);
            } catch (NumberFormatException e) {
                return (N) Long.valueOf(0L);
            }
        } else if (numberClass.isAssignableFrom(Float.class)) {
            try {
                return (N) Float.valueOf(str);
            } catch (NumberFormatException e) {
                return (N) Float.valueOf(0f);
            }
        } else if (numberClass.isAssignableFrom(Double.class)) {
            try {
                return (N) Double.valueOf(str);
            } catch (NumberFormatException e) {
                return (N) Double.valueOf(0d);
            }
        }
        throw new IllegalArgumentException("incorrect number class: " + numberClass);
    }

    public static double mergeDecimals(Collection<? extends Number> numbers) {
        double result = 0;
        int currentMultiplier = 1;
        if (numbers != null) {
            for (Number n : numbers) {
                if (n != null) {
                    result += (n.doubleValue() * (double) currentMultiplier);
                    currentMultiplier *= 10;
                }
            }
        }
        return result;
    }
}

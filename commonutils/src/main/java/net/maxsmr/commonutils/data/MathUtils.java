package net.maxsmr.commonutils.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MathUtils {

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
            List<? extends Number> usedCodesCopy = new ArrayList<>(excludeNumbers);
            for (int i = 0; i < usedCodesCopy.size() && iterations <= rangeSize; i++) {
                Number code = usedCodesCopy.get(i);
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
}

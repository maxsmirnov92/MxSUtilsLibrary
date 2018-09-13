package net.maxsmr.commonutils.data;

import android.support.v4.util.Pair;

import java.util.Arrays;

public class ArgsParser {

    public static Pair<Integer, String> findArg(String[] argsNames, String[] args, int index) {
        return findArg(argsNames, args, index, false);
    }

    public static Pair<Integer, String> findArg(String[] argsNames, String[] args, int index, boolean ignoreCase) {
        if (argsNames == null || argsNames.length == 0) {
            throw new IllegalArgumentException("Incorrect args names: " + Arrays.toString(argsNames));
        }
        if (index < 0 || index >= argsNames.length) {
            throw new IllegalArgumentException("Incorrect arg name index: " + index);
        }
        return args != null?
                Predicate.Methods.findWithIndex(Arrays.asList(args), element -> element != null && (ignoreCase? element.equalsIgnoreCase(argsNames[index]) : element.equals(argsNames[index])))
                : null;
    }

    public static String getPairArg(String args[], Pair<Integer, String> pair) {
        return args != null && pair != null && pair.first != null? getPairArg(args, pair.first) : null;
    }

    public static String getPairArg(String args[], int index) {
        return args != null && index < args.length - 1? args[index + 1] : null;
    }
}

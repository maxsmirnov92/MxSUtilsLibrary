package net.maxsmr.customcontentprovider.sqlite;

import android.content.UriMatcher;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SQLiteUriMatcher {

    public enum UriMatch {

        NO_MATCH(UriMatcher.NO_MATCH), MATCH_ALL(0), MATCH_ID(1);

        private final int value;

        UriMatch(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @NotNull
        public static UriMatch fromValue(int value) {
            for (UriMatch match : UriMatch.values()) {
                if (match.getValue() == value) {
                    return match;
                }
            }
            throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + UriMatch.class.getName());
        }

    }

    public static final class UriMatcherPair {

        public final String authority;
        public final String path;

        public UriMatcherPair(String authority, String path) {
            this.authority = authority;
            this.path = path;
        }

        @Override
        public String toString() {
            return "UriMatcherPair [authority=" + authority + ", path=" + path + "]";
        }

    }

    private final UriMatcher uriMatcher;

    public SQLiteUriMatcher(List<UriMatcherPair> matcherPairs) {

        uriMatcher = new UriMatcher(UriMatch.NO_MATCH.getValue());

        if (matcherPairs != null && !matcherPairs.isEmpty()) {
            for (UriMatcherPair pair : matcherPairs) {
                if (pair == null || pair.authority == null || pair.authority.isEmpty() || pair.path == null || pair.path.isEmpty())
                    continue;

                uriMatcher.addURI(pair.authority, pair.path, UriMatch.MATCH_ALL.getValue());
                uriMatcher.addURI(pair.authority, pair.path + "/#", UriMatch.MATCH_ID.getValue());
            }
        }
    }

    public UriMatch match(Uri uri) {

        if (uri == null || uri.equals(Uri.EMPTY))
            return UriMatch.NO_MATCH;

        return UriMatch.fromValue(uriMatcher.match(uri));
    }

}

package net.maxsmr.customcontentprovider.sqlite;

import android.content.UriMatcher;
import android.net.Uri;

import java.util.List;

public class SQLiteUriMatcher {

    public enum URI_MATCH {

        NO_MATCH(UriMatcher.NO_MATCH), MATCH_ALL(0), MATCH_ID(1);

        private final int value;

        URI_MATCH(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static URI_MATCH fromNativeValue(int value) {
            for (URI_MATCH match : URI_MATCH.values()) {
                if (match.getValue() == value) {
                    return match;
                }
            }
            throw new IllegalArgumentException("Incorrect native value " + value + " for enum type " + URI_MATCH.class.getName());
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

        uriMatcher = new UriMatcher(URI_MATCH.NO_MATCH.getValue());

        if (matcherPairs != null && !matcherPairs.isEmpty()) {
            for (UriMatcherPair pair : matcherPairs) {
                if (pair == null || pair.authority == null || pair.authority.isEmpty() || pair.path == null || pair.path.isEmpty())
                    continue;

                uriMatcher.addURI(pair.authority, pair.path, URI_MATCH.MATCH_ALL.getValue());
                uriMatcher.addURI(pair.authority, pair.path + "/#", URI_MATCH.MATCH_ID.getValue());
            }
        }
    }

    public URI_MATCH match(Uri uri) {

        if (uri == null || uri.equals(Uri.EMPTY))
            return URI_MATCH.NO_MATCH;

        return URI_MATCH.fromNativeValue(uriMatcher.match(uri));
    }

    // public int match(Uri uri) {
    // if (mAuthorities.contains(uri.getAuthority())) {
    // final List<String> pathSegments = uri.getPathSegments();
    // final int pathSegmentsSize = pathSegments.size();
    // if (pathSegmentsSize == 1) {
    // return MATCH_ALL;
    // } else if (pathSegmentsSize == 2 && TextUtils.isDigitsOnly(pathSegments.get(1))) {
    // return MATCH_ID;
    // }
    // }
    // return NO_MATCH;
    // }

}

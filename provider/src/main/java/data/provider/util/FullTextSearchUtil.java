package data.provider.util;

import org.apache.lucene.queryparser.classic.QueryParser;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class FullTextSearchUtil {

    private FullTextSearchUtil() {}

    /**
     * Builds a Lucene query that AND-matches a prefix of every
     * whitespace-separated term in the raw input, e.g. "sen bud" becomes
     * "sen* AND bud*". Terms are escaped so free-text user input can never
     * be interpreted as Lucene query syntax.
     */
    public static String toPrefixQuery(final String rawQuery) {
        return Arrays.stream(rawQuery.trim().split("\\s+"))
                .filter(term -> !term.isEmpty())
                .map(term -> QueryParser.escape(term) + "*")
                .collect(Collectors.joining(" AND "));
    }
}

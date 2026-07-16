package data.provider.exceptions;

import java.util.Arrays;
import java.util.regex.Matcher;

public class BusinessException extends RuntimeException {

    private static final String BRACKETS = "\\{}";

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String messageTemplate, String... args) {
        super(format(messageTemplate, args));
    }

    private static String format(String template, String... args) {
        return Arrays.stream(args)
                .reduce(template, (result, arg) ->
                        result.replaceFirst(BRACKETS, Matcher.quoteReplacement(String.valueOf(arg))));
    }
}
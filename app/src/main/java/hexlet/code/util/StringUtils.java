package hexlet.code.util;

public class StringUtils {
    private static final int MAX_LENGTH = 200;

    public static String truncate(String value) {
        if (value == null) {
            return "";
        }

        if (value.length() <= MAX_LENGTH) {
            return value;
        }

        return value.substring(0, MAX_LENGTH) + "...";
    }
}

package testreport.util;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    /**
     * Escapes a raw log string to HTML-safe output while preserving ANSI color codes.
     */
    public static String escape(String input) {
        if (input == null || input.isEmpty()) return "";

        String colorized = ansiToHtml(input);
        List<String> spanTags = new ArrayList<>();
        String withPlaceholders = preserveSpanTags(colorized, spanTags);
        String escaped = escapeHtmlExceptSpans(withPlaceholders);
        String withRestoredSpans = restoreSpanTags(escaped, spanTags);

        return convertWhitespace(withRestoredSpans);
    }

    /**
     * Converts ANSI escape sequences to HTML <span> tags with CSS classes.
     */
    public static String ansiToHtml(String text) {
        if (text == null) return "";

        return text
                .replace("\u001B[34m", "<span class='ansi-blue'>")
                .replace("\u001B[31m", "<span class='ansi-red'>")
                .replace("\u001B[32m", "<span class='ansi-green'>")
                .replace("\u001B[33m", "<span class='ansi-yellow'>")
                .replace("\u001B[0m", "</span>");
    }

    private static String preserveSpanTags(String input, List<String> spanTags) {
        Matcher spanMatcher = Pattern.compile("<span[^>]*>").matcher(input);
        StringBuffer result = new StringBuffer();
        int index = 0;

        while (spanMatcher.find()) {
            String tag = spanMatcher.group();
            String placeholder = "___SPAN_OPEN_" + index + "___";
            spanTags.add(tag);
            spanMatcher.appendReplacement(result, placeholder);
            index++;
        }
        spanMatcher.appendTail(result);

        return result.toString().replace("</span>", "___SPAN_CLOSE___");
    }

    private static String restoreSpanTags(String input, List<String> spanTags) {
        String output = input;
        for (int i = 0; i < spanTags.size(); i++) {
            output = output.replace("___SPAN_OPEN_" + i + "___", spanTags.get(i));
        }
        return output.replace("___SPAN_CLOSE___", "</span>");
    }

    private static String escapeHtmlExceptSpans(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
    }

    private static String convertWhitespace(String input) {
        Pattern leadingSpaces = Pattern.compile("(?m)^( +)");
        Matcher matcher = leadingSpaces.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String spaces = matcher.group(1).replace(" ", "&nbsp;");
            matcher.appendReplacement(sb, spaces);
        }
        matcher.appendTail(sb);

        return sb.toString().replace("\n", "<br>");
    }
}

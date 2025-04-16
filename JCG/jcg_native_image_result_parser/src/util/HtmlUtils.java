package testreport.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {

    public static String escape(String input) {
        if (input == null || input.isEmpty()) return "";

        // Escape basic characters
        String escaped = input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");

        // Handle leading spaces using Matcher
        Pattern pattern = Pattern.compile("(?m)^( +)");
        Matcher matcher = pattern.matcher(escaped);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String spaces = matcher.group(1).replace(" ", "&nbsp;");
            matcher.appendReplacement(sb, spaces);
        }
        matcher.appendTail(sb);

        // Convert line breaks
        return sb.toString().replace("\n", "<br>");
    }
}

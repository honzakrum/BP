package testreport.parser;

import testreport.model.TestResult;
import testreport.util.HtmlUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class MarkdownParser {

    public void enrichWithMarkdown(Path markdownDir, List<TestResult> results) throws IOException {
        Map<String, TestResult> resultMap = new HashMap<>();
        for (TestResult result : results) {
            resultMap.put(result.getName(), result);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(markdownDir, "*.md")) {
            for (Path mdFile : stream) {
                parseMarkdownFile(mdFile, resultMap);
            }
        }
    }

    private void parseMarkdownFile(Path mdFile, Map<String, TestResult> resultMap) {
        try {
            String content = Files.readString(mdFile);

            // Match: ## TestName\n...content...
            Pattern pattern = Pattern.compile("^## (\\w+)$(.*?)(?=^## \\w+|\\z)", Pattern.MULTILINE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String testName = matcher.group(1).trim();
                String sectionContent = matcher.group(2).trim();

                if (resultMap.containsKey(testName)) {
                    TestResult result = resultMap.get(testName);
                    parseTestContent(sectionContent, result);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading markdown file: " + mdFile + " - " + e.getMessage());
        }
    }

    private void parseTestContent(String content, TestResult result) {
        // Remove hidden markdown comments like: [//]: # (hidden text)
        content = content.replaceAll("(?m)^\\[//\\]:? # \\(.*?\\)\\n?", "");

        int codeStart = content.indexOf("```java");
        if (codeStart < 0) {
            result.setDescription(cleanDescription(content));
            return;
        }

        result.setDescription(cleanDescription(content.substring(0, codeStart)));

        // Extract code blocks
        StringBuilder codeBuilder = new StringBuilder();
        Pattern codeBlockPattern = Pattern.compile("```java\\n(.*?)```", Pattern.DOTALL);
        Matcher codeMatcher = codeBlockPattern.matcher(content);

        while (codeMatcher.find()) {
            String block = codeMatcher.group(1).trim();
            if (!block.isEmpty()) {
                if (codeBuilder.length() > 0) {
                    codeBuilder.append("\n\n");
                }
                codeBuilder.append(block);
            }
        }

        result.setTestCase(HtmlUtils.escape(codeBuilder.toString()));
    }

    private String cleanDescription(String input) {
        return input.trim()
                .replaceAll("`([^`]+)`", "<code>$1</code>")   // Inline code
                .replaceAll("(?m)^[ \\t]*\\r?\\n", "\n")      // Remove trailing empty lines
                .replaceAll("\\n{3,}", "\n\n");               // Normalize line breaks
    }
}

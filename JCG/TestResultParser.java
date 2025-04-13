import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.util.regex.*;

public class TestResultParser {
    private static final String HTML_HEAD = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Test Results Report</title>
            <style>
                body { 
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                    margin: 0; 
                    padding: 20px; 
                    color: #333; 
                    line-height: 1.5;
                }
                .header { 
                    background-color: #2c3e50; 
                    color: white; 
                    padding: 20px; 
                    margin-bottom: 20px; 
                }
                .header h1 {
                    padding-bottom: 10px;
                }
                .summary-stats {
                    background-color: #f8f9fa;
                    padding: 15px;
                    margin-bottom: 20px;
                    border-radius: 4px;
                    border-left: 4px solid #3498db;
                }
                .summary-container { 
                    display: flex; 
                    gap: 15px; 
                    margin-bottom: 30px;
                    flex-wrap: wrap; 
                }
                .summary-card { 
                    flex: 1; 
                    min-width: 200px; 
                    padding: 15px; 
                    border-radius: 4px;
                    cursor: pointer;
                    opacity: 0.9;
                    transition: opacity 0.2s;
                }
                .summary-card.active {
                    opacity: 1;
                    box-shadow: 0 0 0 2px rgba(52, 152, 219, 0.5);
                }
                .summary-card:hover {
                    opacity: 1;
                }
                .passed { background-color: #2ecc71; color: white; }
                .failed { background-color: #e74c3c; color: white; }
                .imprecise { background-color: #f39c12; color: white; }
                .test-list { 
                    display: none; 
                    margin: 15px 0 30px 0;
                }
                .test-item { 
                    padding: 12px 15px;
                    margin: 8px 0;
                    background-color: #f5f5f5;
                    border-left: 4px solid #3498db;
                    cursor: pointer;
                }
                .test-details { 
                    display: none; 
                    padding: 15px; 
                    margin: 10px 0 20px 0; 
                    background-color: #f9f9f9;
                    border: 1px solid #ddd;
                }
                .detail-section { 
                    margin-bottom: 15px; 
                }
                .detail-toggle { 
                    font-weight: bold; 
                    color: #2980b9; 
                    cursor: pointer; 
                    margin-bottom: 8px;
                    display: flex;
                    align-items: center;
                }
                .detail-toggle:before { 
                    content: '▶'; 
                    margin-right: 8px; 
                    font-size: 0.8em;
                }
                .detail-toggle.active:before { 
                    content: '▼'; 
                }
                .detail-content { 
                    display: none; 
                    padding: 10px; 
                    background-color: white; 
                    border: 1px solid #eee;
                    font-family: monospace;
                    font-size: 0.9em;
                    overflow-x: auto;
                }
                .count { 
                    font-size: 1.8em; 
                    font-weight: bold; 
                    margin-bottom: 5px; 
                }
                .status { 
                    font-size: 1em; 
                }
                h1, h2, h3, h4 { margin: 0; }
                .percentage {
                    font-size: 1.2em;
                    font-weight: bold;
                    color: white;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Native Image Test Results</h1>
            
        """;//</div>

    private static final String HTML_FOOTER = """
            <script>
                document.addEventListener('DOMContentLoaded', function() {
                    // All sections collapsed
                    document.querySelectorAll('.test-list, .test-details, .detail-content')
                        .forEach(el => el.style.display = 'none');
                    
                    // Summary
                    const totalTests = %d;
                    const passedTests = %d;
                    const percentage = Math.round((passedTests / totalTests) * 100);
                    document.getElementById('total-tests').textContent = totalTests;
                    document.getElementById('passed-tests-count').textContent = passedTests;
                    document.getElementById('pass-percentage').textContent = percentage + '%%';
                    
                    // Toggle handlers
                    const summaryCards = document.querySelectorAll('.summary-card');
                    summaryCards.forEach(card => {
                        card.addEventListener('click', function() {
                            summaryCards.forEach(c => c.classList.remove('active'));
                            this.classList.add('active');
                            document.querySelectorAll('.test-list') // hide tests in lists
                                .forEach(list => list.style.display = 'none');                        
                            // Show selected test list
                            const category = this.classList.contains('passed') ? 'passed' : 
                                            this.classList.contains('failed') ? 'failed' : 'imprecise';
                            document.getElementById(category + '-tests').style.display = 'block';
                        });
                    });
                    
                    // Test item click handlers
                    document.querySelectorAll('.test-item').forEach(item => {
                        item.addEventListener('click', function() {
                            const testId = this.textContent.trim();
                            const details = document.getElementById(testId + '-details');
                            details.style.display = details.style.display === 'none' ? 'block' : 'none';
                        });
                    });
                    
                    // Detail section toggle handlers
                    document.querySelectorAll('.detail-toggle').forEach(toggle => {
                        toggle.addEventListener('click', function() {
                            this.classList.toggle('active');
                            const sectionId = this.id.replace('-toggle', '-content');
                            document.getElementById(sectionId).style.display = 
                                document.getElementById(sectionId).style.display === 'none' ? 'block' : 'none';
                        });
                    });
                    
                    // Activate failed tests by default
                    document.querySelector('.summary-card.failed').click();
                });
            </script>
        </body>
        </html>
        """;

    private static class TestResult {
        String name;
        String status;
        String description;
        String testCase;
        String log;
        Path jsonPath;
        Path csvMethods;
        Path csvInvokes;
        Path csvTargets;
        Path configFile;
        String matcherOutput;

        public TestResult(String name, String status) {
            this.name = name;
            this.status = status;
            this.description = "No description available";
            this.testCase = "No test case details available";
            this.log = "";
            this.jsonPath = null;
            this.matcherOutput = "";
            this.csvMethods = null;
            this.csvInvokes = null;
            this.csvTargets = null;
            this.configFile = null;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: java TestResultParser <results_file> <log_file> <markdown_dir> [output.html]");
            System.out.println("Default output file: test_results.html");
            return;
        }

        String resultsFile = args[0];
        Path resultsFilePath = Paths.get(resultsFile);
        String resultsDir = resultsFilePath.getParent() != null ?
                resultsFilePath.getParent().toString() : ".";
        String logFile = args[1];
        String markdownDir = args[2];
        String outputFile = args.length > 3 ? args[3] : "test_results.html";

        // Verify files and folder exist
        if (!Files.exists(Paths.get(resultsFile))) {
            System.err.println("Error: Results file not found: " + resultsFile);
            return;
        }
        if (!Files.exists(Paths.get(logFile))) {
            System.err.println("Error: Log file not found: " + logFile);
            return;
        }
        if (!Files.isDirectory(Paths.get(markdownDir))) {
            System.err.println("Error: Markdown directory not found: " + markdownDir);
            return;
        }

        List<TestResult> results = parseResultsFile(resultsFile, resultsDir);
        parseLogFile(logFile, results);
        parseMarkdownFiles(markdownDir, results);
        generateHtmlReport(results, outputFile);

        System.out.println("HTML report generated: " + outputFile);
    }

    private static void parseMarkdownFiles(String dirPath, List<TestResult> results) throws IOException {
        Map<String, TestResult> resultMap = results.stream()
                .collect(Collectors.toMap(r -> r.name, r -> r));

        Files.list(Paths.get(dirPath))
                .filter(path -> path.toString().endsWith(".md"))
                .forEach(mdFile -> parseMarkdownFile(mdFile, resultMap));
    }

    private static void parseMarkdownFile(Path mdFile, Map<String, TestResult> resultMap) {
        try {
            String content = Files.readString(mdFile);
            Pattern testPattern = Pattern.compile("^## (\\w+)$([\\s\\S]+?)(?=^## \\w+|\\z)", Pattern.MULTILINE);
            Matcher matcher = testPattern.matcher(content);

            while (matcher.find()) {
                String testName = matcher.group(1).trim();
                String testContent = matcher.group(2).trim();

                if (resultMap.containsKey(testName)) {
                    TestResult result = resultMap.get(testName);
                    parseTestContent(testContent, result);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading markdown file: " + mdFile + " - " + e.getMessage());
        }
    }

    private static void parseTestContent(String content, TestResult result) {
        // Remove all special comment markers
        content = content.replaceAll("(?m)^\\[//\\]:? # \\(.*?\\)\\n?", "");

        // Extract description
        int codeStart = content.indexOf("```java");
        if (codeStart < 0) { // No java code block
            result.description = cleanDescription(content);
            return;
        }

        result.description = cleanDescription(content.substring(0, codeStart));

        // Extract all java code blocks
        StringBuilder codeBuilder = new StringBuilder();
        Pattern codePattern = Pattern.compile("```java\\n([\\s\\S]+?)```");
        Matcher codeMatcher = codePattern.matcher(content);

        while (codeMatcher.find()) {
            String codeBlock = codeMatcher.group(1).trim();
            if (!codeBlock.isEmpty()) {
                if (codeBuilder.length() > 0) {
                    codeBuilder.append("\n\n");
                }
                codeBuilder.append(codeBlock);
            }
        }

        if (codeBuilder.length() > 0) {
            result.testCase = escapeHtml(codeBuilder.toString());
        }
    }

    private static String cleanDescription(String text) {
        return text.trim()
                .replaceAll("`([^`]+)`", "<code>$1</code>") // Format inline code
                .replaceAll("(?m)^[ \\t]*\\r?\\n", "\n")    // Remove empty lines
                .replaceAll("\\n{3,}", "\n\n");             // Normalize newlines
    }

    private static List<TestResult> parseResultsFile(String filePath, String resultDir) throws IOException {
        string callGraphDir = "./CallGraphs";
        string configDir = "./Config";
        return Files.lines(Paths.get(filePath))
                .map(line -> line.split("\\s+"))
                .filter(parts -> parts.length >= 2)
                .map(parts -> {
                    TestResult result = new TestResult(parts[0], parts[1]);
                    result.jsonPath = Paths.get(resultDir, parts[0], "NativeImage", "PTA", "cg.json");
                    result.csvMethods = Paths.get(callGraphDir, parts[0], "call_tree_methods.csv");
                    result.csvInvokes = Paths.get(callGraphDir, parts[0], "call_tree_invokes.csv");
                    result.csvTargets = Paths.get(callGraphDir, parts[0], "call_tree_targets.csv");
                    result.configFile = Paths.get(configDir, parts[0], "reachability-metadata.json");
                    return result;
                })
                .collect(Collectors.toList());
    }

    private static void parseLogFile(String filePath, List<TestResult> results) throws IOException {
        Map<String, StringBuilder> logMap = new HashMap<>();
        Map<String, StringBuilder> matcherMap = new HashMap<>();
        String currentTest = null;
        StringBuilder currentLog = new StringBuilder();
        StringBuilder currentMatcher = new StringBuilder();
        boolean inMatcherBlock = false;

        for (String line : Files.readAllLines(Paths.get(filePath))) {
            if (line.contains("performing test case:")) {
                if (currentTest != null) {
                    logMap.put(currentTest, new StringBuilder(currentLog));
                    matcherMap.put(currentTest, new StringBuilder(currentMatcher));
                }
                currentTest = line.substring(line.indexOf(":") + 1).trim();
                currentLog = new StringBuilder();
                currentMatcher = new StringBuilder();
                inMatcherBlock = false;
            } else if (currentTest != null) {
                currentLog.append(line).append("\n");

                if (line.contains("[info][CG matcher]")) {
                    inMatcherBlock = true;
                }

                if (inMatcherBlock) {
                    currentMatcher.append(line).append("\n");
                    if (line.trim().isEmpty()) { // end of matcher block
                        inMatcherBlock = false;
                    }
                }
            }
        }

        if (currentTest != null) {
            logMap.put(currentTest, currentLog);
            matcherMap.put(currentTest, currentMatcher);
        }

        for (TestResult result : results) {
            if (logMap.containsKey(result.name)) {
                result.log = escapeHtml(logMap.get(result.name).toString());
            }
            if (matcherMap.containsKey(result.name)) {
                result.matcherOutput = escapeHtml(matcherMap.get(result.name).toString());
            }
        }
    }


    private static String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>")
                .replace("\t", "    ")
                .replace(" ", "&nbsp;");
    }

    private static void generateHtmlReport(List<TestResult> results, String outputFile) throws IOException {
        long passedCount = results.stream().filter(r -> "S".equals(r.status)).count();
        long failedCount = results.stream().filter(r -> "U".equals(r.status)).count();
        long impreciseCount = results.stream().filter(r -> "I".equals(r.status)).count();
        long totalCount = results.size();
        double passPercentage = (passedCount * 100.0) / totalCount;

        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.println(HTML_HEAD);

            // Summary statistics section
            writer.println("<h2>Test Summary</h2>");
            writer.printf("<p><span id='passed-tests-count'>%d</span> of <span id='total-tests'>%d</span> tests passed</p>",
                    passedCount, totalCount);
            writer.printf("<p class='percentage'>Pass rate: <span id='pass-percentage'>%.1f%%</span></p>",
                    passPercentage);
            writer.println("</div>");

            // Summary cards section
            writer.println("<div class='summary-container'>");
            writer.printf("<div class='summary-card passed'>");
            writer.printf("<div class='count'>%d</div><div class='status'>PASSED</div>", passedCount);
            writer.println("</div>");

            writer.printf("<div class='summary-card failed'>");
            writer.printf("<div class='count'>%d</div><div class='status'>FAILED</div>", failedCount);
            writer.println("</div>");

            writer.printf("<div class='summary-card imprecise'>");
            writer.printf("<div class='count'>%d</div><div class='status'>IMPRECISE</div>", impreciseCount);
            writer.println("</div>");
            writer.println("</div>");

            // Test lists by category
            writer.println("<div id='passed-tests' class='test-list'>");
            writer.println("<h3>Passed Tests</h3>");
            results.stream()
                    .filter(r -> "S".equals(r.status))
                    .forEach(r -> writeTestItem(writer, r));
            writer.println("</div>");

            writer.println("<div id='failed-tests' class='test-list'>");
            writer.println("<h3>Failed Tests</h3>");
            results.stream()
                    .filter(r -> "U".equals(r.status))
                    .forEach(r -> writeTestItem(writer, r));
            writer.println("</div>");

            writer.println("<div id='imprecise-tests' class='test-list'>");
            writer.println("<h3>Imprecise Tests</h3>");
            results.stream()
                    .filter(r -> "I".equals(r.status))
                    .forEach(r -> writeTestItem(writer, r));
            writer.println("</div>");

            // Format the footer with actual counts
            String footer = String.format(HTML_FOOTER, totalCount, passedCount);
            writer.println(footer);
        }
    }

    private static boolean validateFilePath(Path p) {
        return p != null && Files.exists(p);
    }

    private static void writeTestItem(PrintWriter writer, TestResult result) {
        writer.printf("<div class='test-item'>%s</div>\n", result.name);

        writer.printf("<div id='%s-details' class='test-details'>\n", result.name);
        writer.printf("<h4>%s</h4>\n", result.name);

        // Description section
        writer.printf("<div class='detail-section'>\n");
        writer.printf("<div id='%s-desc-toggle' class='detail-toggle'>Description</div>\n", result.name);
        writer.printf("<div id='%s-desc-content' class='detail-content'>%s</div>\n",
                result.name, result.description);
        writer.println("</div>");

        // Test case section
        writer.printf("<div class='detail-section'>\n");
        writer.printf("<div id='%s-case-toggle' class='detail-toggle'>Test Case</div>\n", result.name);
        writer.printf("<div id='%s-case-content' class='detail-content'>%s</div>\n",
                result.name, result.testCase);
        writer.println("</div>");

        // Matcher Output section
        if (!result.matcherOutput.isEmpty()) {
            writer.printf("<div class='detail-section'>\n");
            writer.printf("<div id='%s-matcher-toggle' class='detail-toggle'>Matcher Output</div>\n", result.name);
            writer.printf("<div id='%s-matcher-content' class='detail-content'>%s</div>\n",
                    result.name, result.matcherOutput);
            writer.println("</div>");
        }

        // Log section
        writer.printf("<div class='detail-section'>\n");
        writer.printf("<div id='%s-log-toggle' class='detail-toggle'>Debug Log</div>\n", result.name);
        writer.printf("<div id='%s-log-content' class='detail-content'>%s</div>\n",
                result.name, result.log);
        writer.println("</div>");

        // Call Graph
        if (validateFilePath(result.jsonPath)) {
            writer.printf("<div class='detail-section'>\n");
            writer.printf("<a href='%s' target='_blank' style='color: #2980b9; text-decoration: none;'>",
                    result.jsonPath.toString().replace("\\", "/"));
            writer.printf("<div class='detail-toggle'>View serialized Call graph from Native Image</div>");
            writer.printf("</a>");
            writer.println("</div>");
        }

        // Additional files
        if (validateFilePath(result.configFile));
        if (validateFilePath(result.csvMethods));
        if (validateFilePath(result.csvInvokes));
        if (validateFilePath(result.csvTargets));


        writer.println("</div>");
    }
}
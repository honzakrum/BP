import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

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
                .inconclusive { background-color: #f39c12; color: white; }
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
                                            this.classList.contains('failed') ? 'failed' : 'inconclusive';
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

        public TestResult(String name, String status) {
            this.name = name;
            this.status = status;
            this.description = "No description available";
            this.testCase = "No test case details available";
            this.log = "";
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java TestResultParser <results_file> <log_file> [output.html]");
            return;
        }

        String resultsFile = args[0];
        String logFile = args[1];
        String outputFile = args.length > 2 ? args[2] : "test_results.html";

        List<TestResult> results = parseResultsFile(resultsFile);
        parseLogFile(logFile, results);
        generateHtmlReport(results, outputFile);

        System.out.println("HTML report generated: " + outputFile);
    }

    private static List<TestResult> parseResultsFile(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath))
                .map(line -> line.split("\\s+"))
                .filter(parts -> parts.length >= 2)
                .map(parts -> new TestResult(parts[0], parts[1]))
                .collect(Collectors.toList());
    }

    private static void parseLogFile(String filePath, List<TestResult> results) throws IOException {
        Map<String, StringBuilder> logMap = new HashMap<>();
        String currentTest = null;
        StringBuilder currentLog = new StringBuilder();

        for (String line : Files.readAllLines(Paths.get(filePath))) {
            if (line.contains("performing test case:")) {
                if (currentTest != null) {
                    logMap.put(currentTest, currentLog);
                    currentLog = new StringBuilder();
                }
                currentTest = line.substring(line.indexOf(":") + 1).trim();
            } else if (currentTest != null) {
                currentLog.append(line).append("\n");
            }
        }

        if (currentTest != null) {
            logMap.put(currentTest, currentLog);
        }

        for (TestResult result : results) {
            if (logMap.containsKey(result.name)) {
                result.log = escapeHtml(logMap.get(result.name).toString());
            }
        }
    }

    private static String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    private static void generateHtmlReport(List<TestResult> results, String outputFile) throws IOException {
        long passedCount = results.stream().filter(r -> "S".equals(r.status)).count();
        long failedCount = results.stream().filter(r -> "U".equals(r.status)).count();
        long inconclusiveCount = results.stream().filter(r -> "I".equals(r.status)).count();
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

            writer.printf("<div class='summary-card inconclusive'>");
            writer.printf("<div class='count'>%d</div><div class='status'>INCONCLUSIVE</div>", inconclusiveCount);
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

            writer.println("<div id='inconclusive-tests' class='test-list'>");
            writer.println("<h3>Inconclusive Tests</h3>");
            results.stream()
                    .filter(r -> "I".equals(r.status))
                    .forEach(r -> writeTestItem(writer, r));
            writer.println("</div>");

            // Format the footer with actual counts
            String footer = String.format(HTML_FOOTER, totalCount, passedCount);
            writer.println(footer);
        }
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

        // Log section
        writer.printf("<div class='detail-section'>\n");
        writer.printf("<div id='%s-log-toggle' class='detail-toggle'>Debug Log</div>\n", result.name);
        writer.printf("<div id='%s-log-content' class='detail-content'>%s</div>\n",
                result.name, result.log);
        writer.println("</div>");

        writer.println("</div>");
    }
}
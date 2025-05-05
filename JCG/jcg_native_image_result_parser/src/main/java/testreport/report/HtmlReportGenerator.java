package testreport.report;

import testreport.model.TestResult;
import testreport.model.TestStatus;
import testreport.util.HtmlUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates an interactive HTML report from a list of parsed test results.
 *
 * The report includes summary statistics, categorized test listings (Passed, Failed, Imprecise),
 * expandable details for each test case (including logs, descriptions, and links to artifacts),
 * and dynamic filtering using JavaScript.
 *
 * This class assumes the presence of structured data provided via [[TestResult]] objects,
 * enriched by logs and markdown annotations, and supports direct links to serialized
 * call graphs and configuration files.
 *
 * @author Jan Křůmal
 */
public class HtmlReportGenerator {

    private static final String NO_TESTS = "<p class='no-tests'>No test cases matching this category.</p>";

    public void generate(List<TestResult> results, Path outputFile) throws IOException {
        long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
        long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
        long imprecise = results.stream().filter(r -> r.getStatus() == TestStatus.IMPRECISE).count();
        long total = results.size();
        double percentage = (passed * 100.0) / total;

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            writeHead(writer);
            writeSummary(writer, passed, total, percentage);
            writeCards(writer, passed, failed, imprecise);
            writeTestLists(writer, results);
            writeFooter(writer, total, passed);
        }
    }

    private void writeHead(PrintWriter writer) {
        writer.println("""
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
                .no-tests {
                        color: #666;
                        font-style: italic;
                        padding: 15px;
                        background-color: #f5f5f5;
                        border-left: 4px solid #ddd;
                    }
                .ansi-blue { color: #4f81bd; }
                .ansi-red { color: #c00000; }
                .ansi-green { color: #00a000; }
                .ansi-yellow { color: #b58900; }
                
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
        """);
    }

    private void writeSummary(PrintWriter writer, long passed, long total, double percentage) {
        writer.println("<h2>Test Summary</h2>");
        writer.printf("<p><span id='passed-tests-count'>%d</span> of <span id='total-tests'>%d</span> tests passed</p>%n", passed, total);
        writer.printf("<p class='percentage'>Pass rate: <span id='pass-percentage'>%.1f%%</span></p>%n", percentage);
        writer.println("</div>");
    }

    private void writeCards(PrintWriter writer, long passed, long failed, long imprecise) {
        writer.println("<div class='summary-container'>");

        writer.printf("<div class='summary-card passed'><div class='count'>%d</div><div class='status'>PASSED</div></div>%n", passed);
        writer.printf("<div class='summary-card failed'><div class='count'>%d</div><div class='status'>FAILED</div></div>%n", failed);
        writer.printf("<div class='summary-card imprecise'><div class='count'>%d</div><div class='status'>IMPRECISE</div></div>%n", imprecise);

        writer.println("</div>");
    }

    private void writeTestLists(PrintWriter writer, List<TestResult> results) {
        writeCategory(writer, "passed-tests", "Passed Tests", results, TestStatus.PASSED);
        writeCategory(writer, "failed-tests", "Failed Tests", results, TestStatus.FAILED);
        writeCategory(writer, "imprecise-tests", "Imprecise Tests", results, TestStatus.IMPRECISE);
    }

    private void writeCategory(PrintWriter writer, String id, String title, List<TestResult> results, TestStatus status) {
        writer.printf("<div id='%s' class='test-list'>%n", id);
        writer.printf("<h3>%s</h3>%n", title);
        List<TestResult> filtered = results.stream().filter(r -> r.getStatus() == status).toList();

        if (filtered.isEmpty()) {
            writer.println(NO_TESTS);
        } else {
            filtered.forEach(r -> writeTestItem(writer, r));
        }

        writer.println("</div>");
    }

    private void writeTestItem(PrintWriter writer, TestResult r) {
        writer.printf("<div class='test-item'>%s</div>%n", r.getName());
        writer.printf("<div id='%s-details' class='test-details'>%n", r.getName());
        writer.printf("<h4>%s</h4>%n", r.getName());

        writeToggleSection(writer, r.getName(), "Description", r.getDescription());
        writeToggleSection(writer, r.getName(), "Test Case", r.getTestCase());

        if (!r.getMatcherOutput().isEmpty()) {
            writeToggleSection(writer, r.getName(), "Matcher Output", r.getMatcherOutput());
        }

        writeToggleSection(writer, r.getName(), "Debug Log", r.getLog());

        // JSON call graph
        if (isValidFile(r.getJsonPath())) {
            writer.printf("<div class='detail-section'>%n");
            writer.printf("<div id='%s-callgraph-toggle' class='detail-toggle'>Call Graph</div>%n", r.getName());
            writer.printf("<div id='%s-callgraph-content' class='detail-content'>%n", r.getName());
            writer.println("<p>Call graph from Native Image serialized into JSON:</p>");
            writer.printf("<a href='%s' target='_blank'>%s</a>%n",
                    r.getJsonPath().toString().replace("\\", "/"),
                    r.getJsonPath().getFileName());
            writer.println("</div></div>");
        }

        // Additional Files
        writeAdditionalFiles(writer, r);

        writer.println("</div>"); // end test-details
    }

    private void writeToggleSection(PrintWriter writer, String testName, String label, String content) {
        String idBase = testName + "-" + label.toLowerCase().replace(" ", "-");
        writer.printf("<div class='detail-section'>%n");
        writer.printf("<div id='%s-toggle' class='detail-toggle'>%s</div>%n", idBase, label);
        writer.printf("<div id='%s-content' class='detail-content'>%s</div>%n", idBase, content);
        writer.println("</div>");
    }

    private void writeAdditionalFiles(PrintWriter writer, TestResult r) {
        writer.printf("<div class='detail-section'>%n");
        writer.printf("<div id='%s-files-toggle' class='detail-toggle'>Additional Files</div>%n", r.getName());
        writer.printf("<div id='%s-files-content' class='detail-content'>%n", r.getName());

        writer.println("<div class='file-group'>");
        writer.println("<p>Config generated for native image:</p>");
        if (isValidFile(r.getConfigFile())) {
            writer.printf("<a href='%s' target='_blank'>%s</a>%n",
                    r.getConfigFile().toString().replace("\\", "/"),
                    r.getConfigFile().getFileName());
        } else {
            writer.println("Configuration file not found.");
        }
        writer.println("</div>");

        writer.println("<div class='file-group'>");
        writer.println("<p>Call graph analysis files (CSV):</p>");
        writer.println("<ul>");
        writeCsvLink(writer, r.getCsvMethods(), "Methods in call graph");
        writeCsvLink(writer, r.getCsvInvokes(), "Method invocation relationships");
        writeCsvLink(writer, r.getCsvTargets(), "Target methods");
        writer.println("</ul></div>");

        writer.println("</div></div>");
    }

    private void writeCsvLink(PrintWriter writer, Path filePath, String description) {
        if (isValidFile(filePath)) {
            writer.printf("<li><a href='%s' target='_blank'>%s</a> - %s</li>%n",
                    filePath.toString().replace("\\", "/"),
                    filePath.getFileName(),
                    description);
        } else {
            writer.printf("<li>%s - file not generated</li>%n", description);
        }
    }

    private boolean isValidFile(Path path) {
        return path != null && Files.exists(path);
    }

    private void writeFooter(PrintWriter writer, long total, long passed) {
        writer.printf("""
            <script>
            document.addEventListener('DOMContentLoaded', function() {
                document.querySelectorAll('.test-list, .test-details, .detail-content')
                    .forEach(el => el.style.display = 'none');

                document.getElementById('total-tests').textContent = %d;
                document.getElementById('passed-tests-count').textContent = %d;
                document.getElementById('pass-percentage').textContent =
                    Math.round((%d / %d) * 100) + '%%';

                document.querySelectorAll('.summary-card').forEach(card => {
                    card.addEventListener('click', function() {
                        document.querySelectorAll('.summary-card').forEach(c => c.classList.remove('active'));
                        this.classList.add('active');

                        document.querySelectorAll('.test-list').forEach(list => list.style.display = 'none');
                        const category = this.classList.contains('passed') ? 'passed' :
                                         this.classList.contains('failed') ? 'failed' : 'imprecise';
                        document.getElementById(category + '-tests').style.display = 'block';
                    });
                });

                document.querySelectorAll('.test-item').forEach(item => {
                    item.addEventListener('click', function() {
                        const details = document.getElementById(this.textContent.trim() + '-details');
                        if (details) {
                            details.style.display = details.style.display === 'none' ? 'block' : 'none';
                        }
                    });
                });

                document.querySelectorAll('.detail-toggle').forEach(toggle => {
                    toggle.addEventListener('click', function() {
                        this.classList.toggle('active');
                        const sectionId = this.id.replace('-toggle', '-content');
                        const section = document.getElementById(sectionId);
                        if (section) {
                            section.style.display = section.style.display === 'none' ? 'block' : 'none';
                        }
                    });
                });

                document.querySelector('.summary-card.failed').click();
            });
            </script>
        </body></html>
        """, total, passed, passed, total);
    }
}

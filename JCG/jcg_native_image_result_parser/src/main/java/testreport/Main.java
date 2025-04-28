package testreport;

import testreport.model.TestResult;
import testreport.parser.LogParser;
import testreport.parser.MarkdownParser;
import testreport.parser.ResultParser;
import testreport.report.HtmlReportGenerator;

import java.nio.file.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar app.jar <results_file> <log_file> <markdown_dir> [output.html]");
            return;
        }

        Path resultsPath = Paths.get(args[0]);
        Path logPath = Paths.get(args[1]);
        Path markdownDir = Paths.get(args[2]);
        Path outputPath = args.length > 3 ? Paths.get(args[3]) : Paths.get("test_results.html");

        // Sanity check
        if (!Files.exists(resultsPath)) {
            System.err.println("Error: Results file not found: " + resultsPath);
            return;
        }
        if (!Files.exists(logPath)) {
            System.err.println("Error: Log file not found: " + logPath);
            return;
        }
        if (!Files.isDirectory(markdownDir)) {
            System.err.println("Error: Markdown directory not found: " + markdownDir);
            return;
        }

        try {
            ResultParser resultParser = new ResultParser();
            LogParser logParser = new LogParser();
            MarkdownParser markdownParser = new MarkdownParser();
            HtmlReportGenerator reportGenerator = new HtmlReportGenerator();

            List<TestResult> results = resultParser.parse(resultsPath);
            logParser.enrichWithLogs(logPath, results);
            markdownParser.enrichWithMarkdown(markdownDir, results);
            reportGenerator.generate(results, outputPath);

            System.out.println("HTML report generated at: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

package testreport.parser;

import testreport.model.TestResult;
import testreport.model.TestStatus;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses the evaluation results file and maps entries to [[TestResult]] objects.
 *
 * Each line of the results file is expected to contain a test name and status code.
 * Constructs paths to associated artifacts (e.g., call graph JSON, CSV files, config)
 * based on predefined directory structure.
 *
 * @author Jan Křůmal
 */
public class ResultParser {

    private static final Path CALL_GRAPH_DIR = Paths.get("./CallGraphs");
    private static final Path CONFIG_DIR = Paths.get("./config");

    public List<TestResult> parse(Path resultsPath) throws IOException {
        String resultDir = resultsPath.getParent() != null ?
                resultsPath.getParent().toString() : ".";

        return Files.lines(resultsPath)                                          // Read lines from file
                .map(String::trim)                                              // Trim whitespace
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))       // Skip empty lines & comments
                .map(line -> line.split("\\s+"))                                // Split line into words
                .filter(parts -> parts.length >= 2)                             // Make sure we have at least test name + status
                .map(parts -> createTestResult(parts[0], parts[1], resultDir))  // Build TestResult
                .collect(Collectors.toList());                                  // Collect to list
    }

    private TestResult createTestResult(String testName, String statusCode, String resultDir) {
        TestStatus status = TestStatus.fromCode(statusCode);
        TestResult result = new TestResult(testName, status);

        result.setJsonPath(Paths.get(resultDir, testName, "NativeImage", "PTA", "cg.json"));
        result.setCsvMethods(CALL_GRAPH_DIR.resolve(testName).resolve("call_tree_methods.csv"));
        result.setCsvInvokes(CALL_GRAPH_DIR.resolve(testName).resolve("call_tree_invokes.csv"));
        result.setCsvTargets(CALL_GRAPH_DIR.resolve(testName).resolve("call_tree_targets.csv"));
        result.setConfigFile(CONFIG_DIR.resolve(testName).resolve("reachability-metadata.json"));

        return result;
    }
}
